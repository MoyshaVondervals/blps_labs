package ru.itmo.searchcourierservice.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.searchcourierservice.dto.NotificationEvent;
import ru.itmo.searchcourierservice.model.entity.OutboxEvent;
import ru.itmo.searchcourierservice.model.enums.OutboxEventStatus;
import ru.itmo.searchcourierservice.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String sourceService;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:2000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findBySourceServiceAndStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                        sourceService,
                        OutboxEventStatus.NEW,
                        LocalDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    @Transactional
    public void publishEvent(OutboxEvent event) {
        try {
            NotificationEvent payload = objectMapper.readValue(event.getPayload(), NotificationEvent.class);
            kafkaTemplate.executeInTransaction(operations -> {
                try {
                    operations.send(event.getTopic(), event.getEventKey(), payload).get();
                    return null;
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to send outbox event to Kafka", e);
                }
            });

            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
        } catch (Exception e) {
            event.setAttempts(event.getAttempts() + 1);
            event.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(60, event.getAttempts() * 5L)));
            event.setLastError(e.getMessage());
            outboxEventRepository.save(event);
            log.warn("Failed to publish outbox event {}", event.getId(), e);
        }
    }
}
