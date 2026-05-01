package org.moysha.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.moysha.notificationservice.model.enums.RecipientType;
import org.moysha.notificationservice.service.SseEmitterService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseNotificationController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/notifications/{recipientType}/{recipientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable RecipientType recipientType,
                                @PathVariable Long recipientId) {
        return sseEmitterService.subscribe(recipientType, recipientId);
    }
}

