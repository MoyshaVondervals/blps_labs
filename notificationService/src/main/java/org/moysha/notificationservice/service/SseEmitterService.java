package org.moysha.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moysha.notificationservice.dto.NotificationEvent;
import org.moysha.notificationservice.model.entity.Notification;
import org.moysha.notificationservice.model.enums.RecipientType;
import org.moysha.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository;

    public SseEmitter subscribe(RecipientType recipientType, Long recipientId) {
        String key = buildKey(recipientType, recipientId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(ex -> removeEmitter(key, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE подключение установлено для " + recipientType + " #" + recipientId));
        } catch (IOException e) {
            System.out.println("Не удалось отправить начальное SSE-событие для "+ key);
            removeEmitter(key, emitter);
        }

        System.out.println("SSE подписка создана: "+ key);
        return emitter;
    }
    @Transactional
    public void sendNotification(RecipientType recipientType, Long recipientId,
                                 NotificationEvent notificationEvent) {
        Notification notification = saveNotification(mapNotification(notificationEvent));

        String key = buildKey(recipientType, recipientId);
        List<SseEmitter> recipientEmitters = emitters.get(key);

        if (recipientEmitters == null || recipientEmitters.isEmpty()) {
            System.err.println("Нет активных SSE-подписчиков для "+ key);
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : recipientEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                System.err.println("SSE-соединение потеряно для "+ key);
                deadEmitters.add(emitter);
            }
        }

        for (SseEmitter dead : deadEmitters) {
            removeEmitter(key, dead);
        }

    }


    private Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    private Notification mapNotification(NotificationEvent notificationEvent) {
        Notification notification = Notification.builder()
                .recipientType(notificationEvent.recipientType())
                .recipientId(notificationEvent.recipientId())
                .orderId(notificationEvent.orderId())
                .message(notificationEvent.message())
                .isRead(notificationEvent.isRead())
                .build();
        return notification;
    }


    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> recipientEmitters = emitters.get(key);
        if (recipientEmitters != null) {
            recipientEmitters.remove(emitter);
            if (recipientEmitters.isEmpty()) {
                emitters.remove(key);
            }
            System.out.println("SSE-эмиттер удалён для "+ key);
        }
    }

    private String buildKey(RecipientType recipientType, Long recipientId) {
        return recipientType.name() + ":" + recipientId;
    }
}
