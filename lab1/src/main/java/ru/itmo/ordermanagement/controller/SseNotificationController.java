package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ru.itmo.ordermanagement.model.enums.RecipientType;
import ru.itmo.ordermanagement.service.SseEmitterService;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE Notifications", description = "Server-Sent Events для получения уведомлений в реальном времени")
public class SseNotificationController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/notifications/{recipientType}/{recipientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Подписаться на SSE-уведомления",
            description = "Устанавливает SSE-соединение для получения уведомлений в реальном времени. " +
                    "recipientType: CUSTOMER, SELLER, COURIER. " +
                    "При появлении новых уведомлений они будут отправлены клиенту через этот поток. " +
                    "Событие 'connected' — подтверждение подключения. " +
                    "Событие 'notification' — новое уведомление.")
    public SseEmitter subscribe(
            @PathVariable RecipientType recipientType,
            @PathVariable Long recipientId) {
        return sseEmitterService.subscribe(recipientType, recipientId);
    }
}

