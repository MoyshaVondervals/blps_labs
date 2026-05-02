package ru.itmo.ordermanagement.service.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.dto.SearchCourierRequest;

@Service
@RequiredArgsConstructor
public class SearchCourierPublisher {
    @Value("${topic.search-courier}")
    private String searchCourierTopic;

    private final KafkaTemplate<String, SearchCourierRequest> kafkaTemplate;

    public void publish(SearchCourierRequest request) {
        kafkaTemplate.send(searchCourierTopic, String.valueOf(request.getOrderId()), request);
    }
}

