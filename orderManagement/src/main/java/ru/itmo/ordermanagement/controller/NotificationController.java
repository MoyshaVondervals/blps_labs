package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.NotificationEvent;
import ru.itmo.ordermanagement.model.enums.RecipientType;
import ru.itmo.ordermanagement.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Уведомления участникам бизнес-процесса")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{recipientType}/{recipientId}")
    @Operation(summary = "Получить все уведомления получателя",
            description = "recipientType: CUSTOMER, SELLER, COURIER")
    public ResponseEntity<Page<NotificationEvent>> getNotifications(
            @PathVariable RecipientType recipientType,
            @PathVariable Long recipientId,
            @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(recipientType, recipientId, pageable));
    }

    @GetMapping("/{recipientType}/{recipientId}/unread")
    @Operation(summary = "Получить непрочитанные уведомления получателя")
    public ResponseEntity<Page<NotificationEvent>> getUnreadNotifications(
            @PathVariable RecipientType recipientType,
            @PathVariable Long recipientId,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(recipientType, recipientId, pageable));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Отметить уведомление как прочитанное")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }
}
