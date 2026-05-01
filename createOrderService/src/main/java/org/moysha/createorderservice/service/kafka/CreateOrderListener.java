package org.moysha.createorderservice.service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moysha.createorderservice.dto.CreateOrderRequest;
import org.moysha.createorderservice.service.CreateOrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderListener {

    private final CreateOrderService createOrderService;

    @KafkaListener(
            topics = "${topic.create-order}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"spring.json.trusted.packages=*"}
    )
    public void onMessage(CreateOrderRequest request) {
        createOrderService.createOrder(request);
        log.info("Create order request processed for customer #{}", request.getCustomerId());
    }
}

