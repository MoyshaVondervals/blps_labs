package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ru.itmo.ordermanagement.model.enums.RecipientType;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseProxyService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final WebClient.Builder webClientBuilder;

    @Value("${app.notification-service.base-url}")
    private String notificationServiceBaseUrl;

    public SseEmitter subscribe(RecipientType recipientType, Long recipientId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        WebClient client = webClientBuilder.baseUrl(notificationServiceBaseUrl).build();

        Flux<ServerSentEvent<String>> eventFlux = client.get()
                .uri("/api/sse/notifications/{recipientType}/{recipientId}", recipientType, recipientId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

        Disposable subscription = eventFlux.subscribe(event -> {
            try {
                if (event.event() != null) {
                    emitter.send(SseEmitter.event()
                            .name(event.event())
                            .data(event.data()));
                } else {
                    emitter.send(event.data());
                }
            } catch (IOException e) {
                log.debug("SSE proxy send failed", e);
                emitter.completeWithError(e);
            }
        }, emitter::completeWithError, emitter::complete);

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(ex -> subscription.dispose());

        return emitter;
    }
}

