package com.inventory.dto;

import com.inventory.model.Notification;
import com.inventory.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long equipmentId,
        String equipmentName,
        Long reservationId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getEquipmentId(),
                n.getEquipmentName(),
                n.getReservationId(),
                n.getStartTime(),
                n.getEndTime(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }
}
