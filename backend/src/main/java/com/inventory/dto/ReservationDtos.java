package com.inventory.dto;

import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.model.WaitlistEntry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    public record ReservationRequest(
            @NotNull(message = "Ekipman seçilmelidir")
            Long equipmentId,

            @NotNull(message = "Başlangıç zamanı zorunludur")
            LocalDateTime startTime,

            @NotNull(message = "Bitiş zamanı zorunludur")
            LocalDateTime endTime
    ) {
    }

    /** Red, iptal veya iade sırasında yazılan isteğe bağlı not. */
    public record NoteRequest(
            @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
            String note
    ) {
    }

    public record ReturnRequest(
            @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
            String note,

            // true ise ekipman "Arızalı" durumuna alınır
            boolean reportFault
    ) {
    }

    public record ReservationResponse(
            Long id,
            Long equipmentId,
            String equipmentName,
            String serialNumber,
            Long userId,
            String userFullName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ReservationStatus status,
            String note,
            LocalDateTime checkedOutAt,
            LocalDateTime returnedAt,
            boolean overdue,
            boolean returnedLate,
            LocalDateTime createdAt
    ) {
        public static ReservationResponse from(Reservation reservation, LocalDateTime now) {
            return new ReservationResponse(
                    reservation.getId(),
                    reservation.getEquipment().getId(),
                    reservation.getEquipment().getName(),
                    reservation.getEquipment().getSerialNumber(),
                    reservation.getUser().getId(),
                    reservation.getUser().getFullName(),
                    reservation.getStartTime(),
                    reservation.getEndTime(),
                    reservation.getStatus(),
                    reservation.getNote(),
                    reservation.getCheckedOutAt(),
                    reservation.getReturnedAt(),
                    reservation.isOverdue(now),
                    reservation.isReturnedLate(),
                    reservation.getCreatedAt()
            );
        }
    }

    public record WaitlistResponse(
            Long id,
            Long equipmentId,
            String equipmentName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            // Aralık şu anda boş mu (rezervasyon yapılabilir mi)
            boolean slotAvailable,
            LocalDateTime createdAt
    ) {
        public static WaitlistResponse from(WaitlistEntry entry, boolean slotAvailable) {
            return new WaitlistResponse(
                    entry.getId(),
                    entry.getEquipment().getId(),
                    entry.getEquipment().getName(),
                    entry.getStartTime(),
                    entry.getEndTime(),
                    slotAvailable,
                    entry.getCreatedAt()
            );
        }
    }
}
