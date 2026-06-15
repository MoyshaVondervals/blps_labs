package ru.itmo.searchcourierservice.service.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.searchcourierservice.dto.CourierAssignedEvent;

@Service
@RequiredArgsConstructor
public class CourierAssignedEventPublisher {

    @Value("${topic.courier-assigned}")
    private String courierAssignedTopic;

    private final KafkaTemplate<String, CourierAssignedEvent> kafkaTemplate;

    public void publish(CourierAssignedEvent event) {
        kafkaTemplate.send(courierAssignedTopic, String.valueOf(event.getOrderId()), event);
    }
}
