package ru.itmo.ordermanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.itmo.ordermanagement.dto.NotificationResponse;
import ru.itmo.ordermanagement.model.enums.RecipientType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

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
            log.warn("Не удалось отправить начальное SSE-событие для {}", key);
            removeEmitter(key, emitter);
        }

        log.info("SSE подписка создана: {}", key);
        return emitter;
    }

    public void sendNotification(RecipientType recipientType, Long recipientId,
                                 NotificationResponse notification) {
        String key = buildKey(recipientType, recipientId);
        List<SseEmitter> recipientEmitters = emitters.get(key);

        if (recipientEmitters == null || recipientEmitters.isEmpty()) {
            log.debug("Нет активных SSE-подписчиков для {}", key);
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : recipientEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.debug("SSE-соединение потеряно для {}", key);
                deadEmitters.add(emitter);
            }
        }

        for (SseEmitter dead : deadEmitters) {
            removeEmitter(key, dead);
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> recipientEmitters = emitters.get(key);
        if (recipientEmitters != null) {
            recipientEmitters.remove(emitter);
            if (recipientEmitters.isEmpty()) {
                emitters.remove(key);
            }
            log.debug("SSE-эмиттер удалён для {}", key);
        }
    }

    private String buildKey(RecipientType recipientType, Long recipientId) {
        return recipientType.name() + ":" + recipientId;
    }
}
