package ru.itmo.searchcourierservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.itmo.searchcourierservice.model.enums.RecipientType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private UUID eventId;
    private Long id;
    private RecipientType recipientType;
    private Long recipientId;
    private Long orderId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
