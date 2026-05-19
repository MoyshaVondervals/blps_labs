package ru.itmo.searchcourierservice.service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import ru.itmo.searchcourierservice.dto.SearchCourierRequest;
import ru.itmo.searchcourierservice.service.SearchCourierService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchCourierListener {

    private final SearchCourierService searchCourierService;

    @KafkaListener(
            topics = "${topic.search-courier}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"spring.json.trusted.packages=*"}
    )
    public void onMessage(SearchCourierRequest request, Acknowledgment ack) {
        searchCourierService.searchCourier(request);
        log.info("Search courier request processed for order #{}", request.getOrderId());
        ack.acknowledge();
    }
}

