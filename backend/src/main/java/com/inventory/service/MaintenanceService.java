package com.inventory.service;

import com.inventory.dto.EquipmentDtos.MaintenanceRequest;
import com.inventory.dto.EquipmentDtos.MaintenanceResponse;
import com.inventory.exception.BusinessException;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.FieldChange;
import com.inventory.model.MaintenanceRecord;
import com.inventory.model.MaintenanceType;
import com.inventory.model.User;
import com.inventory.repository.MaintenanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ekipmanların bakım, kalibrasyon ve onarım geçmişi.
 */
@Service
public class MaintenanceService {

    private static final Map<MaintenanceType, String> TYPE_LABELS = Map.of(
            MaintenanceType.MAINTENANCE, "Bakım",
            MaintenanceType.CALIBRATION, "Kalibrasyon",
            MaintenanceType.REPAIR, "Onarım");

    private final MaintenanceRecordRepository maintenanceRepository;
    private final EquipmentService equipmentService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public MaintenanceService(MaintenanceRecordRepository maintenanceRepository,
                              EquipmentService equipmentService,
                              UserService userService,
                              AuditLogService auditLogService) {
        this.maintenanceRepository = maintenanceRepository;
        this.equipmentService = equipmentService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getHistory(Long equipmentId) {
        equipmentService.findActive(equipmentId);
        return maintenanceRepository.findByEquipmentIdOrderByPerformedDateDescIdDesc(equipmentId).stream()
                .map(MaintenanceResponse::from)
                .toList();
    }

    /**
     * Yeni bakım kaydı ekler.
     * Kalibrasyon kaydında "sonraki tarih" girilmişse ekipmanın kalibrasyon tarihi de güncellenir.
     */
    @Transactional
    public MaintenanceResponse add(Long equipmentId, MaintenanceRequest request, Long currentUserId) {
        Equipment equipment = equipmentService.findActive(equipmentId);
        User currentUser = userService.getById(currentUserId);

        if (request.nextDueDate() != null && !request.nextDueDate().isAfter(request.performedDate())) {
            throw new BusinessException("Sonraki tarih, işlem tarihinden sonra olmalıdır");
        }

        MaintenanceRecord record = maintenanceRepository.save(new MaintenanceRecord(
                equipment,
                request.type(),
                request.performedDate(),
                request.nextDueDate(),
                EquipmentService.normalize(request.performedBy()),
                EquipmentService.normalize(request.description()),
                currentUser));

        String label = TYPE_LABELS.get(request.type());
        auditLogService.record(AuditAction.CREATE, AuditEntityType.MAINTENANCE, equipment.getId(),
                equipment.getName(), currentUser.getFullName(),
                label + " kaydı eklendi (" + request.performedDate() + ")");

        if (request.type() == MaintenanceType.CALIBRATION && request.nextDueDate() != null
                && !Objects.equals(equipment.getCalibrationDate(), request.nextDueDate())) {
            String old = Objects.toString(equipment.getCalibrationDate(), null);
            equipment.setCalibrationDate(request.nextDueDate());
            auditLogService.record(AuditAction.UPDATE, AuditEntityType.EQUIPMENT, equipment.getId(),
                    equipment.getName(), currentUser.getFullName(),
                    List.of(new FieldChange("calibrationDate", old, request.nextDueDate().toString())),
                    "Kalibrasyon kaydından güncellendi");
        }

        if (request.markAvailable()) {
            equipmentService.changeStatus(equipment, EquipmentStatus.AVAILABLE,
                    label + " tamamlandı", currentUser.getFullName());
        }

        return MaintenanceResponse.from(record);
    }
}
