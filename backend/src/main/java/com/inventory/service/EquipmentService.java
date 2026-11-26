package com.inventory.service;

import com.inventory.dto.EquipmentDtos.EquipmentFilters;
import com.inventory.dto.EquipmentDtos.EquipmentOption;
import com.inventory.dto.EquipmentDtos.EquipmentRequest;
import com.inventory.dto.EquipmentDtos.EquipmentResponse;
import com.inventory.dto.EquipmentDtos.StatusUpdateRequest;
import com.inventory.dto.PageResponse;
import com.inventory.exception.BusinessException;
import com.inventory.exception.NotFoundException;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.FieldChange;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.model.User;
import com.inventory.repository.EquipmentRepository;
import com.inventory.repository.ReservationRepository;
import com.inventory.repository.WaitlistRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;
    private final WaitlistRepository waitlistRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final Clock clock;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            ReservationRepository reservationRepository,
                            WaitlistRepository waitlistRepository,
                            AuditLogService auditLogService,
                            UserService userService,
                            NotificationService notificationService,
                            Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
        this.waitlistRepository = waitlistRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** Liste arama kriterleri; boş (null) alanlar filtrelemeye katılmaz. */
    public record Filter(String q, String category, String location, EquipmentStatus status) {
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> search(Filter filter, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name", "id"));
        return PageResponse.from(equipmentRepository.findAll(toSpec(filter), pageable), EquipmentResponse::from);
    }

    /** Filtreye uyan tüm aktif ekipmanlar (Excel / PDF dışa aktarma için). */
    @Transactional(readOnly = true)
    public List<Equipment> findAll(Filter filter) {
        return equipmentRepository.findAll(toSpec(filter), Sort.by("name", "id"));
    }

    @Transactional(readOnly = true)
    public List<EquipmentOption> getOptions() {
        return equipmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(EquipmentOption::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentFilters getFilters() {
        return new EquipmentFilters(equipmentRepository.findDistinctCategories(),
                equipmentRepository.findDistinctLocations());
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getById(Long id) {
        return EquipmentResponse.from(findActive(id));
    }

    @Transactional(readOnly = true)
    public Equipment findActive(Long id) {
        return equipmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Ekipman bulunamadı"));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request, Long currentUserId) {
        return EquipmentResponse.from(createEntity(request, userService.getById(currentUserId)));
    }

    /** Ekipmanı oluşturur ve audit kaydını yazar (Excel içe aktarma da bunu kullanır). */
    @Transactional
    public Equipment createEntity(EquipmentRequest request, User currentUser) {
        if (equipmentRepository.existsBySerialNumberIgnoreCaseAndActiveTrue(request.serialNumber().trim())) {
            throw new BusinessException("Bu seri numarasına sahip bir ekipman zaten var", HttpStatus.CONFLICT);
        }

        Equipment equipment = new Equipment();
        applyRequest(equipment, request);
        equipment.setCreatedBy(currentUser);
        equipmentRepository.save(equipment);

        auditLogService.record(AuditAction.CREATE, AuditEntityType.EQUIPMENT, equipment.getId(),
                equipment.getName(), currentUser.getFullName(), null);
        return equipment;
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request, Long currentUserId) {
        Equipment equipment = findActive(id);
        if (equipmentRepository.existsBySerialNumberIgnoreCaseAndActiveTrueAndIdNot(request.serialNumber().trim(), id)) {
            throw new BusinessException("Bu seri numarasına sahip başka bir ekipman var", HttpStatus.CONFLICT);
        }

        List<FieldChange> changes = diff(equipment, request);
        if (!changes.isEmpty()) {
            applyRequest(equipment, request);
            String performedBy = userService.getById(currentUserId).getFullName();
            auditLogService.record(AuditAction.UPDATE, AuditEntityType.EQUIPMENT, equipment.getId(),
                    equipment.getName(), performedBy, changes, null);
        }
        return EquipmentResponse.from(equipment);
    }

    /**
     * Ekipmanın durumunu değiştirir (ör. Arızalı, Bakımda).
     * Kullanılabilir olmayan ekipman için yeni rezervasyon yapılamaz ve teslim alınamaz.
     */
    @Transactional
    public EquipmentResponse changeStatus(Long id, StatusUpdateRequest request, Long currentUserId) {
        Equipment equipment = findActive(id);
        changeStatus(equipment, request.status(), normalize(request.note()),
                userService.getById(currentUserId).getFullName());
        return EquipmentResponse.from(equipment);
    }

    @Transactional
    public void changeStatus(Equipment equipment, EquipmentStatus newStatus, String note, String performedBy) {
        EquipmentStatus old = equipment.getStatus();
        if (old == newStatus) {
            return;
        }
        equipment.setStatus(newStatus);
        auditLogService.record(AuditAction.STATUS_CHANGE, AuditEntityType.EQUIPMENT, equipment.getId(),
                equipment.getName(), performedBy,
                List.of(new FieldChange("status", old.name(), newStatus.name())), note);
    }

    /**
     * Ekipmanı pasife alır (kayıt silinmez, raporlarda görünmeye devam eder),
     * henüz başlamamış rezervasyonlarını iptal eder ve bekleme listesini temizler.
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        Equipment equipment = findActive(id);
        equipment.setActive(false);
        String performedBy = userService.getById(currentUserId).getFullName();

        List<Reservation> future = reservationRepository.findByEquipmentIdAndStatusInAndStartTimeAfter(
                id, ReservationStatus.BLOCKING, LocalDateTime.now(clock));
        for (Reservation r : future) {
            r.setStatus(ReservationStatus.CANCELLED);
            r.setNote("Ekipman envanterden kaldırıldı");
            notificationService.reservationCancelledByOther(r, performedBy);
        }
        waitlistRepository.deleteByEquipmentId(id);

        auditLogService.record(AuditAction.DELETE, AuditEntityType.EQUIPMENT, equipment.getId(),
                equipment.getName(), performedBy,
                future.isEmpty() ? null : future.size() + " ileri tarihli rezervasyon iptal edildi");
    }

    private static void applyRequest(Equipment equipment, EquipmentRequest request) {
        equipment.setName(request.name().trim());
        equipment.setBrand(request.brand().trim());
        equipment.setModel(request.model().trim());
        equipment.setSerialNumber(request.serialNumber().trim());
        equipment.setCategory(normalize(request.category()));
        equipment.setLocation(normalize(request.location()));
        equipment.setCalibrationDate(request.calibrationDate());
        equipment.setDescription(normalize(request.description()));
        if (request.status() != null) {
            equipment.setStatus(request.status());
        }
        if (request.requiresApproval() != null) {
            equipment.setRequiresApproval(request.requiresApproval());
        }
    }

    /**
     * Mevcut ekipman ile güncelleme isteği arasındaki farkları bulur.
     */
    static List<FieldChange> diff(Equipment current, EquipmentRequest request) {
        List<FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "name", current.getName(), request.name().trim());
        addIfChanged(changes, "brand", current.getBrand(), request.brand().trim());
        addIfChanged(changes, "model", current.getModel(), request.model().trim());
        addIfChanged(changes, "serialNumber", current.getSerialNumber(), request.serialNumber().trim());
        addIfChanged(changes, "category", current.getCategory(), normalize(request.category()));
        addIfChanged(changes, "location", current.getLocation(), normalize(request.location()));
        addIfChanged(changes, "calibrationDate",
                Objects.toString(current.getCalibrationDate(), null),
                Objects.toString(request.calibrationDate(), null));
        addIfChanged(changes, "description", current.getDescription(), normalize(request.description()));
        if (request.status() != null) {
            addIfChanged(changes, "status", current.getStatus().name(), request.status().name());
        }
        if (request.requiresApproval() != null) {
            addIfChanged(changes, "requiresApproval",
                    String.valueOf(current.isRequiresApproval()), String.valueOf(request.requiresApproval()));
        }
        return changes;
    }

    private static void addIfChanged(List<FieldChange> changes, String field, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue));
        }
    }

    // Boş metinler null olarak saklanır
    static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static Specification<Equipment> toSpec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("brand")), like),
                        cb.like(cb.lower(root.get("model")), like),
                        cb.like(cb.lower(root.get("serialNumber")), like)));
            }
            if (f.category() != null && !f.category().isBlank()) {
                predicates.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.location() != null && !f.location().isBlank()) {
                predicates.add(cb.equal(root.get("location"), f.location()));
            }
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
