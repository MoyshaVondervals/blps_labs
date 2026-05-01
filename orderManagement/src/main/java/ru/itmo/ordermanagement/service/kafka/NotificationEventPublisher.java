package ru.itmo.ordermanagement.service.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.dto.NotificationEvent;

@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {
    @Value("${topic.send-notification}")
    private String sendClientTopic;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void publish(NotificationEvent notificationEvent) {
        kafkaTemplate.send(sendClientTopic, notificationEvent.getRecipientType()+":"+ notificationEvent.getRecipientId(), notificationEvent);
    }

}
