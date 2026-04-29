package org.moysha.notificationservice.dto;

import org.moysha.notificationservice.model.enums.RecipientType;

import java.time.LocalDateTime;

public record NotificationEvent(
        Long id,
        RecipientType recipientType,
        Long recipientId,
        Long orderId,
        String message,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
