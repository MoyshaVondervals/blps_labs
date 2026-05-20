package org.moysha.notificationservice.dto;

import org.moysha.notificationservice.model.enums.RecipientType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        Long id,
        RecipientType recipientType,
        Long recipientId,
        Long orderId,
        String message,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
