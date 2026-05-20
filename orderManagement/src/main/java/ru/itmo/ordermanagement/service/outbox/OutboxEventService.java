package ru.itmo.ordermanagement.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.model.entity.OutboxEvent;
import ru.itmo.ordermanagement.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String sourceService;

    public void saveEvent(String aggregateType, Long aggregateId, String eventType,
                          String topic, String eventKey, Object payload) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .sourceService(sourceService)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .eventKey(eventKey)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
