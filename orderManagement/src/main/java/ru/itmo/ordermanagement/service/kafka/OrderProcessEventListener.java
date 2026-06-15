package ru.itmo.ordermanagement.service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.dto.CourierAssignedEvent;
import ru.itmo.ordermanagement.dto.OrderCreatedEvent;
import ru.itmo.ordermanagement.service.OrderDomainService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessEventListener {

    private final RuntimeService runtimeService;
    private final OrderDomainService orderDomainService;

    @KafkaListener(
            topics = "${topic.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=ru.itmo.ordermanagement.dto.OrderCreatedEvent",
                    "spring.json.trusted.packages=ru.itmo.ordermanagement.dto"
            }
    )
    public void onOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        if (event.getRequestId() == null) {
            log.warn("OrderCreated event ignored because requestId is missing: order #{}", event.getOrderId());
            ack.acknowledge();
            return;
        }

        try {
            MessageCorrelationResult result = runtimeService.createMessageCorrelation("OrderCreated")
                    .processInstanceVariableEquals("requestId", event.getRequestId().toString())
                    .setVariable("orderId", event.getOrderId())
                    .setVariable("customerId", event.getCustomerId())
                    .setVariable("sellerId", event.getSellerId())
                    .correlateWithResult();

            String processInstanceId = result.getExecution().getProcessInstanceId();
            orderDomainService.attachProcessInstance(event.getOrderId(), processInstanceId);
            log.info("Order #{} correlated with process instance {}", event.getOrderId(), processInstanceId);
            ack.acknowledge();
        } catch (MismatchingMessageCorrelationException e) {
            log.warn("No waiting process instance found for OrderCreated requestId {}", event.getRequestId(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${topic.courier-assigned}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=ru.itmo.ordermanagement.dto.CourierAssignedEvent",
                    "spring.json.trusted.packages=ru.itmo.ordermanagement.dto"
            }
    )
    public void onCourierAssigned(CourierAssignedEvent event, Acknowledgment ack) {
        try {
            runtimeService.createMessageCorrelation("CourierAssigned")
                    .processInstanceVariableEquals("orderId", event.getOrderId())
                    .setVariable("courierId", event.getCourierId())
                    .correlateWithResult();

            log.info("Courier #{} correlated with order process #{}", event.getCourierId(), event.getOrderId());
            ack.acknowledge();
        } catch (MismatchingMessageCorrelationException e) {
            log.warn("No waiting process instance found for CourierAssigned order #{}", event.getOrderId(), e);
            throw e;
        }
    }
}
