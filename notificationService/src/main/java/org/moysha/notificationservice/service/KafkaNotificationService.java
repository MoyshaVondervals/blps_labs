package org.moysha.notificationservice.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moysha.notificationservice.dto.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationService {
    private static final String TOPIC = "${app.topic.send-notification}";
    private static final String GROUP_ID = "${spring.kafka.consumer.group-id}";

    private final SseEmitterService sseEmitterService;


    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID,
            properties = {"spring.json.trusted.packages=*"}
    )
    public void onMessage(NotificationEvent payload, Acknowledgment ack) {
        sseEmitterService.sendNotification(payload.recipientType(), payload.recipientId(), payload);
        ack.acknowledge();
    }
}
