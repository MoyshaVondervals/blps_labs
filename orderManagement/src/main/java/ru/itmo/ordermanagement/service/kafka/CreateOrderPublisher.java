package ru.itmo.ordermanagement.service.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.dto.CreateOrderRequest;

@Service
@RequiredArgsConstructor
public class CreateOrderPublisher {
    @Value("${topic.create-order}")
    private String createOrderTopic;

    private final KafkaTemplate<String, CreateOrderRequest> kafkaTemplate;

    public void publish(CreateOrderRequest createOrderRequest) {
        kafkaTemplate.send(createOrderTopic, String.valueOf(createOrderRequest.getCustomerId()), createOrderRequest);
    }
}
