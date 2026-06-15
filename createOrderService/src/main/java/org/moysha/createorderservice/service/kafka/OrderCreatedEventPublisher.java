package org.moysha.createorderservice.service.kafka;

import lombok.RequiredArgsConstructor;
import org.moysha.createorderservice.dto.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCreatedEventPublisher {

    @Value("${topic.order-created}")
    private String orderCreatedTopic;

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.getRequestId()), event);
    }
}
