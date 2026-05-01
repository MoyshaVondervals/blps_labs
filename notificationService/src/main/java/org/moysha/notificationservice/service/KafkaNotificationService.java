package org.moysha.notificationservice.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moysha.notificationservice.dto.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationService {
    private static final String TOPIC = "${app.topic.send-notification}";
    private static final String GROUP_ID = "${spring.kafka.consumer.group-id}";

    private final SseEmitterService sseEmitterService;


    @Transactional
    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP_ID,
            properties = {"spring.json.trusted.packages=*"}
    )
    public void onMessage(NotificationEvent payload) {
        sseEmitterService.sendNotification(payload.recipientType(), payload.recipientId(), payload);
    }
}
