package com.inventory.dto;

import com.inventory.model.Equipment;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.MaintenanceRecord;
import com.inventory.model.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EquipmentDtos {

    private EquipmentDtos() {
    }

    public record EquipmentRequest(
            @NotBlank(message = "Ekipman adı zorunludur")
            @Size(max = 100)
            String name,

            @NotBlank(message = "Marka zorunludur")
            @Size(max = 100)
            String brand,

            @NotBlank(message = "Model zorunludur")
            @Size(max = 100)
            String model,

            @NotBlank(message = "Seri numarası zorunludur")
            @Size(max = 100)
            String serialNumber,

            @Size(max = 50, message = "Kategori en fazla 50 karakter olabilir")
            String category,

            @Size(max = 100, message = "Konum en fazla 100 karakter olabilir")
            String location,

            LocalDate calibrationDate,

            @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
            String description,

            // Boş bırakılırsa: yeni ekipmanda AVAILABLE, güncellemede mevcut durum korunur
            EquipmentStatus status,

            // Boş bırakılırsa: yeni ekipmanda false, güncellemede mevcut değer korunur
            Boolean requiresApproval
    ) {
    }

    public record EquipmentResponse(
            Long id,
            String name,
            String brand,
            String model,
            String serialNumber,
            String category,
            String location,
            LocalDate calibrationDate,
            String description,
            EquipmentStatus status,
            boolean requiresApproval,
            LocalDateTime createdAt,
            String createdBy
    ) {
        public static EquipmentResponse from(Equipment equipment) {
            return new EquipmentResponse(
                    equipment.getId(),
                    equipment.getName(),
                    equipment.getBrand(),
                    equipment.getModel(),
                    equipment.getSerialNumber(),
                    equipment.getCategory(),
                    equipment.getLocation(),
                    equipment.getCalibrationDate(),
                    equipment.getDescription(),
                    equipment.getStatus(),
                    equipment.isRequiresApproval(),
                    equipment.getCreatedAt(),
                    equipment.getCreatedBy().getFullName()
            );
        }
    }

    /** Açılır listeler (rezervasyon formu, filtreler) için hafif ekipman bilgisi. */
    public record EquipmentOption(
            Long id,
            String name,
            String serialNumber,
            EquipmentStatus status,
            boolean requiresApproval
    ) {
        public static EquipmentOption from(Equipment equipment) {
            return new EquipmentOption(equipment.getId(), equipment.getName(), equipment.getSerialNumber(),
                    equipment.getStatus(), equipment.isRequiresApproval());
        }
    }

    /** Filtre açılır listeleri için mevcut kategori ve konumlar. */
    public record EquipmentFilters(List<String> categories, List<String> locations) {
    }

    public record StatusUpdateRequest(
            @NotNull(message = "Durum zorunludur")
            EquipmentStatus status,

            @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
            String note
    ) {
    }

    public record ImportResult(int created, int failed, List<RowError> errors) {
        public record RowError(int row, String message) {
        }
    }

    public record MaintenanceRequest(
            @NotNull(message = "İşlem türü zorunludur")
            MaintenanceType type,

            @NotNull(message = "İşlem tarihi zorunludur")
            @PastOrPresent(message = "İşlem tarihi ileri bir tarih olamaz")
            LocalDate performedDate,

            LocalDate nextDueDate,

            @Size(max = 100, message = "En fazla 100 karakter olabilir")
            String performedBy,

            @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
            String description,

            // true ise ekipman tekrar "Kullanılabilir" durumuna alınır
            boolean markAvailable
    ) {
    }

    public record MaintenanceResponse(
            Long id,
            Long equipmentId,
            MaintenanceType type,
            LocalDate performedDate,
            LocalDate nextDueDate,
            String performedBy,
            String description,
            String createdBy,
            LocalDateTime createdAt
    ) {
        public static MaintenanceResponse from(MaintenanceRecord record) {
            return new MaintenanceResponse(
                    record.getId(),
                    record.getEquipment().getId(),
                    record.getType(),
                    record.getPerformedDate(),
                    record.getNextDueDate(),
                    record.getPerformedBy(),
                    record.getDescription(),
                    record.getCreatedBy().getFullName(),
                    record.getCreatedAt()
            );
        }
    }
}
