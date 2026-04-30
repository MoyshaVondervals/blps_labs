package org.moysha.createorderservice.service.kafka;

import lombok.RequiredArgsConstructor;
import org.moysha.createorderservice.dto.NotificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {
    @Value("${topic.send-notification}")
    private String sendNotificationTopic;

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void publish(NotificationEvent notificationEvent) {
        String key = notificationEvent.getRecipientType() + ":" + notificationEvent.getRecipientId();
        kafkaTemplate.send(sendNotificationTopic, key, notificationEvent);
    }
}

