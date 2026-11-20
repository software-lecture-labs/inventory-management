package com.inventory.service;

import com.inventory.dto.PageResponse;
import com.inventory.dto.ReservationDtos.ReservationRequest;
import com.inventory.dto.ReservationDtos.ReservationResponse;
import com.inventory.dto.ReservationDtos.ReturnRequest;
import com.inventory.dto.ReservationDtos.WaitlistResponse;
import com.inventory.exception.BusinessException;
import com.inventory.exception.NotFoundException;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.model.User;
import com.inventory.model.WaitlistEntry;
import com.inventory.repository.ReservationRepository;
import com.inventory.repository.WaitlistRepository;
import com.inventory.security.UserPrincipal;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReservationService {

    /** PDF raporuna en fazla bu kadar kayıt yazılır. */
    public static final int REPORT_LIMIT = 5000;

    // Takvimde gösterilen durumlar (reddedilen ve iptal edilenler gösterilmez)
    private static final Set<ReservationStatus> CALENDAR_STATUSES = EnumSet.of(
            ReservationStatus.PENDING, ReservationStatus.APPROVED,
            ReservationStatus.CHECKED_OUT, ReservationStatus.RETURNED);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static final Map<EquipmentStatus, String> STATUS_LABELS = Map.of(
            EquipmentStatus.AVAILABLE, "Kullanılabilir",
            EquipmentStatus.OUT_OF_ORDER, "Arızalı",
            EquipmentStatus.MAINTENANCE, "Bakımda",
            EquipmentStatus.CALIBRATION, "Kalibrasyonda");

    private final ReservationRepository reservationRepository;
    private final WaitlistRepository waitlistRepository;
    private final EquipmentService equipmentService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final int checkOutEarlyMinutes;

    public ReservationService(ReservationRepository reservationRepository,
                              WaitlistRepository waitlistRepository,
                              EquipmentService equipmentService,
                              UserService userService,
                              AuditLogService auditLogService,
                              NotificationService notificationService,
                              Clock clock,
                              @Value("${app.reservation.check-out-early-minutes:30}") int checkOutEarlyMinutes) {
        this.reservationRepository = reservationRepository;
        this.waitlistRepository = waitlistRepository;
        this.equipmentService = equipmentService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.clock = clock;
        this.checkOutEarlyMinutes = checkOutEarlyMinutes;
    }

    /** Rapor / liste arama kriterleri; boş (null) alanlar filtrelemeye katılmaz. */
    public record Filter(Long userId, String user, String equipment, ReservationStatus status,
                         LocalDate from, LocalDate to, boolean overdueOnly) {
    }

    // ================= Sorgular =================

    /** Takvim için: verilen aralıkla kesişen rezervasyonlar. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getCalendar(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = now();
        LocalDateTime rangeFrom = from != null ? from : now.minusMonths(1);
        LocalDateTime rangeTo = to != null ? to : now.plusMonths(2);
        return reservationRepository.findInRange(rangeFrom, rangeTo, CALENDAR_STATUSES).stream()
                .map(r -> ReservationResponse.from(r, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> search(Filter filter, int page, int size) {
        LocalDateTime now = now();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime", "id"));
        return PageResponse.from(reservationRepository.findAll(toSpec(filter, now), pageable),
                r -> ReservationResponse.from(r, now));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findForReport(Filter filter) {
        LocalDateTime now = now();
        var pageable = PageRequest.of(0, REPORT_LIMIT, Sort.by(Sort.Direction.DESC, "startTime", "id"));
        return reservationRepository.findAll(toSpec(filter, now), pageable)
                .map(r -> ReservationResponse.from(r, now))
                .getContent();
    }

    // ================= Oluşturma ve onay =================

    /**
     * Yeni rezervasyon. Ekipman onay gerektiriyorsa (ve isteyen yönetici değilse) PENDING olarak oluşur.
     */
    @Transactional
    public ReservationResponse create(ReservationRequest request, UserPrincipal currentUser) {
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();
        validateRange(start, end);

        Equipment equipment = equipmentService.findActive(request.equipmentId());
        ensureAvailable(equipment);
        if (reservationRepository.existsOverlapping(equipment.getId(), start, end, ReservationStatus.BLOCKING)) {
            throw new BusinessException("Ekipman bu zaman aralığında zaten rezerve edilmiş", HttpStatus.CONFLICT);
        }

        User user = userService.getById(currentUser.id());
        Reservation reservation = new Reservation(equipment, user, start, end);
        boolean needsApproval = equipment.isRequiresApproval() && !currentUser.isAdmin();
        reservation.setStatus(needsApproval ? ReservationStatus.PENDING : ReservationStatus.APPROVED);
        Reservation saved = reservationRepository.save(reservation);

        // Kullanıcı bu ekipman için çakışan bir aralıkta bekliyorduysa artık beklemesine gerek yok
        waitlistRepository.deleteForUserOverlapping(equipment.getId(), user.getId(), start, end);

        audit(AuditAction.CREATE, saved, user.getFullName(), needsApproval ? "Onay bekliyor" : null);
        notificationService.reservationCreated(saved);
        return ReservationResponse.from(saved, now());
    }

    @Transactional
    public ReservationResponse approve(Long id, UserPrincipal admin) {
        Reservation r = find(id);
        requireStatus(r, ReservationStatus.PENDING, "Sadece onay bekleyen rezervasyonlar onaylanabilir");
        if (!r.getEndTime().isAfter(now())) {
            throw new BusinessException("Süresi geçmiş bir talep onaylanamaz");
        }
        r.setStatus(ReservationStatus.APPROVED);
        audit(AuditAction.APPROVE, r, admin.fullName(), null);
        notificationService.reservationApproved(r);
        return ReservationResponse.from(r, now());
    }

    @Transactional
    public ReservationResponse reject(Long id, String note, UserPrincipal admin) {
        Reservation r = find(id);
        requireStatus(r, ReservationStatus.PENDING, "Sadece onay bekleyen rezervasyonlar reddedilebilir");
        r.setStatus(ReservationStatus.REJECTED);
        r.setNote(EquipmentService.normalize(note));
        audit(AuditAction.REJECT, r, admin.fullName(), r.getNote());
        notificationService.reservationRejected(r);
        releaseSlot(r.getEquipment(), r.getStartTime(), r.getEndTime());
        return ReservationResponse.from(r, now());
    }

    /**
     * Rezervasyonu iptal eder. Kullanıcı sadece kendi rezervasyonunu, yönetici ise hepsini iptal edebilir.
     * Teslim alınmış bir rezervasyon iptal edilemez; önce iade edilmelidir.
     */
    @Transactional
    public ReservationResponse cancel(Long id, UserPrincipal currentUser, String note) {
        Reservation r = find(id);
        boolean isOwner = ensureOwnerOrAdmin(r, currentUser, "Bu rezervasyonu iptal etme yetkiniz yok");
        if (r.getStatus() != ReservationStatus.PENDING && r.getStatus() != ReservationStatus.APPROVED) {
            throw new BusinessException(r.getStatus() == ReservationStatus.CHECKED_OUT
                    ? "Teslim alınmış bir rezervasyon iptal edilemez, ekipmanı iade edin"
                    : "Bu rezervasyon iptal edilemez");
        }

        r.setStatus(ReservationStatus.CANCELLED);
        r.setNote(EquipmentService.normalize(note));
        audit(AuditAction.CANCEL, r, currentUser.fullName(), r.getNote());
        if (!isOwner) {
            notificationService.reservationCancelledByOther(r, currentUser.fullName());
        }

        LocalDateTime now = now();
        LocalDateTime freedFrom = r.getStartTime().isAfter(now) ? r.getStartTime() : now;
        releaseSlot(r.getEquipment(), freedFrom, r.getEndTime());
        return ReservationResponse.from(r, now);
    }

    // ================= Teslim alma / iade =================

    @Transactional
    public ReservationResponse checkOut(Long id, UserPrincipal currentUser) {
        Reservation r = find(id);
        ensureOwnerOrAdmin(r, currentUser, "Bu rezervasyonu teslim alma yetkiniz yok");
        if (r.getStatus() == ReservationStatus.PENDING) {
            throw new BusinessException("Rezervasyon henüz onaylanmadı");
        }
        requireStatus(r, ReservationStatus.APPROVED, "Bu rezervasyon teslim alınamaz");

        LocalDateTime now = now();
        if (now.isBefore(r.getStartTime().minusMinutes(checkOutEarlyMinutes))) {
            throw new BusinessException("Ekipmanı en erken başlangıçtan " + checkOutEarlyMinutes
                    + " dakika önce teslim alabilirsiniz");
        }
        if (!now.isBefore(r.getEndTime())) {
            throw new BusinessException("Rezervasyon süresi dolmuş");
        }
        ensureAvailable(r.getEquipment());

        r.setStatus(ReservationStatus.CHECKED_OUT);
        r.setCheckedOutAt(now);
        audit(AuditAction.CHECK_OUT, r, currentUser.fullName(), null);
        return ReservationResponse.from(r, now);
    }

    /**
     * Ekipmanı iade eder. Arıza bildirilirse ekipman "Arızalı" durumuna alınır.
     * Erken iade edilirse kalan süre boşalır ve bekleme listesindekilere haber verilir.
     */
    @Transactional
    public ReservationResponse returnEquipment(Long id, UserPrincipal currentUser, ReturnRequest request) {
        Reservation r = find(id);
        ensureOwnerOrAdmin(r, currentUser, "Bu rezervasyonu iade etme yetkiniz yok");
        requireStatus(r, ReservationStatus.CHECKED_OUT, "Sadece teslim alınmış ekipman iade edilebilir");

        LocalDateTime now = now();
        String note = request == null ? null : EquipmentService.normalize(request.note());
        boolean reportFault = request != null && request.reportFault();

        r.setStatus(ReservationStatus.RETURNED);
        r.setReturnedAt(now);
        r.setNote(note);

        String auditNote = joinNotes(r.isReturnedLate() ? "Geç iade" : null,
                reportFault ? "Arıza bildirildi" : null, note);
        audit(AuditAction.RETURN, r, currentUser.fullName(), auditNote);

        if (reportFault) {
            equipmentService.changeStatus(r.getEquipment(), EquipmentStatus.OUT_OF_ORDER,
                    note != null ? note : "İade sırasında arıza bildirildi", currentUser.fullName());
        } else if (now.isBefore(r.getEndTime())) {
            releaseSlot(r.getEquipment(), now, r.getEndTime());
        }
        return ReservationResponse.from(r, now);
    }

    // ================= Bekleme listesi =================

    @Transactional
    public WaitlistResponse joinWaitlist(ReservationRequest request, UserPrincipal currentUser) {
        validateRange(request.startTime(), request.endTime());
        Equipment equipment = equipmentService.findActive(request.equipmentId());

        if (!reservationRepository.existsOverlapping(equipment.getId(), request.startTime(), request.endTime(),
                ReservationStatus.BLOCKING)) {
            throw new BusinessException("Bu zaman aralığı şu anda boş, doğrudan rezervasyon yapabilirsiniz");
        }
        if (waitlistRepository.existsByEquipmentIdAndUserIdAndStartTimeAndEndTime(
                equipment.getId(), currentUser.id(), request.startTime(), request.endTime())) {
            throw new BusinessException("Bu aralık için zaten bekleme listesindesiniz", HttpStatus.CONFLICT);
        }

        User user = userService.getById(currentUser.id());
        WaitlistEntry entry = waitlistRepository.save(
                new WaitlistEntry(equipment, user, request.startTime(), request.endTime()));
        return WaitlistResponse.from(entry, false);
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getMyWaitlist(Long userId) {
        return waitlistRepository.findActiveForUser(userId, now()).stream()
                .map(w -> WaitlistResponse.from(w, isSlotFree(w)))
                .toList();
    }

    @Transactional
    public void leaveWaitlist(Long id, UserPrincipal currentUser) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bekleme listesi kaydı bulunamadı"));
        if (!entry.getUser().getId().equals(currentUser.id()) && !currentUser.isAdmin()) {
            throw new AccessDeniedException("Bu kaydı silme yetkiniz yok");
        }
        waitlistRepository.delete(entry);
    }

    /**
     * [from, to) aralığı boşaldığında, bekleme listesinde bu aralıkla kesişen ve
     * istediği aralık tamamen boş olan kullanıcılara (ilk gelen önce) haber verir.
     */
    void releaseSlot(Equipment equipment, LocalDateTime from, LocalDateTime to) {
        if (!equipment.isActive() || equipment.getStatus() != EquipmentStatus.AVAILABLE || !from.isBefore(to)) {
            return;
        }
        LocalDateTime now = now();
        List<WaitlistEntry> notified = new ArrayList<>();
        for (WaitlistEntry entry : waitlistRepository.findWaiting(equipment.getId(), from, to, now)) {
            boolean clashesWithNotified = notified.stream().anyMatch(n ->
                    n.getStartTime().isBefore(entry.getEndTime()) && n.getEndTime().isAfter(entry.getStartTime()));
            if (clashesWithNotified || !isSlotFree(entry)) {
                continue;
            }
            // Haber verilen kişi listeden çıkarılır; rezervasyon için gereken bilgiler bildirimde kalır
            notificationService.waitlistSlotAvailable(entry);
            waitlistRepository.delete(entry);
            notified.add(entry);
        }
    }

    // ================= Zamanlanmış işler (ReservationScheduler çağırır) =================

    /** Önümüzdeki [minutes] dakika içinde başlayacak onaylı rezervasyonlar için hatırlatma gönderir. */
    @Transactional
    public int sendReminders(int minutes) {
        LocalDateTime now = now();
        List<Reservation> upcoming = reservationRepository.findNeedingReminder(now, now.plusMinutes(minutes));
        for (Reservation r : upcoming) {
            notificationService.reservationReminder(r);
            r.setReminderSent(true);
        }
        return upcoming.size();
    }

    /** İade süresi geçmiş rezervasyonların sahiplerini uyarır (her rezervasyon için bir kez). */
    @Transactional
    public int notifyOverdue() {
        List<Reservation> overdue = reservationRepository.findOverdueNotNotified(now());
        for (Reservation r : overdue) {
            notificationService.reservationOverdue(r);
            r.setOverdueNotified(true);
        }
        return overdue.size();
    }

    @Transactional
    public int cleanupWaitlist() {
        return waitlistRepository.deleteExpired(now());
    }

    /** Engellenen kullanıcının ileri tarihli rezervasyonlarını iptal eder. */
    @Transactional
    public int cancelFutureForUser(Long userId, String performedBy, String reason) {
        List<Reservation> future = reservationRepository.findByUserIdAndStatusInAndStartTimeAfter(
                userId, EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED), now());
        for (Reservation r : future) {
            r.setStatus(ReservationStatus.CANCELLED);
            r.setNote(reason);
            audit(AuditAction.CANCEL, r, performedBy, reason);
            releaseSlot(r.getEquipment(), r.getStartTime(), r.getEndTime());
        }
        return future.size();
    }

    // ================= yardımcılar =================

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Reservation find(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rezervasyon bulunamadı"));
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessException("Bitiş zamanı başlangıç zamanından sonra olmalıdır");
        }
        if (start.isBefore(now())) {
            throw new BusinessException("Geçmiş bir zamana rezervasyon yapılamaz");
        }
    }

    private static void ensureAvailable(Equipment equipment) {
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new BusinessException("Bu ekipman şu anda kullanılamaz durumda ("
                    + STATUS_LABELS.get(equipment.getStatus()) + ")");
        }
    }

    private static void requireStatus(Reservation r, ReservationStatus expected, String message) {
        if (r.getStatus() != expected) {
            throw new BusinessException(message);
        }
    }

    /** Sahibi veya yönetici değilse 403 fırlatır; sahibi ise true döner. */
    private static boolean ensureOwnerOrAdmin(Reservation r, UserPrincipal currentUser, String message) {
        boolean isOwner = r.getUser().getId().equals(currentUser.id());
        if (!isOwner && !currentUser.isAdmin()) {
            throw new AccessDeniedException(message);
        }
        return isOwner;
    }

    private boolean isSlotFree(WaitlistEntry entry) {
        return entry.getEquipment().isActive()
                && entry.getEquipment().getStatus() == EquipmentStatus.AVAILABLE
                && !reservationRepository.existsOverlapping(entry.getEquipment().getId(),
                entry.getStartTime(), entry.getEndTime(), ReservationStatus.BLOCKING);
    }

    private void audit(AuditAction action, Reservation r, String performedBy, String extraNote) {
        String period = r.getUser().getFullName() + ", "
                + r.getStartTime().format(TIME_FORMAT) + " - " + r.getEndTime().format(TIME_FORMAT);
        auditLogService.record(action, AuditEntityType.RESERVATION, r.getId(), r.getEquipment().getName(),
                performedBy, joinNotes(period, extraNote));
    }

    private static String joinNotes(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" · ");
                }
                sb.append(part);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static Specification<Reservation> toSpec(Filter f, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> user = root.join("user", JoinType.INNER);
            Join<Object, Object> equipment = root.join("equipment", JoinType.INNER);

            if (f.userId() != null) {
                predicates.add(cb.equal(user.get("id"), f.userId()));
            }
            if (f.user() != null && !f.user().isBlank()) {
                String like = "%" + f.user().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(
                        cb.lower(cb.concat(cb.concat(user.get("firstName"), " "), user.get("lastName"))), like));
            }
            if (f.equipment() != null && !f.equipment().isBlank()) {
                String like = "%" + f.equipment().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(equipment.get("name")), like),
                        cb.like(cb.lower(equipment.get("serialNumber")), like)));
            }
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), f.from().atStartOfDay()));
            }
            if (f.to() != null) {
                predicates.add(cb.lessThan(root.get("startTime"), f.to().plusDays(1).atStartOfDay()));
            }
            if (f.overdueOnly()) {
                predicates.add(cb.equal(root.get("status"), ReservationStatus.CHECKED_OUT));
                predicates.add(cb.lessThan(root.get("endTime"), now));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
