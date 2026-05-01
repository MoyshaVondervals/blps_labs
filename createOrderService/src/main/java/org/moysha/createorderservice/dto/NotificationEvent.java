package org.moysha.createorderservice.dto;

import lombok.Builder;
import lombok.Data;
import org.moysha.createorderservice.model.enums.RecipientType;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationEvent {
    private Long id;
    private RecipientType recipientType;
    private Long recipientId;
    private Long orderId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

