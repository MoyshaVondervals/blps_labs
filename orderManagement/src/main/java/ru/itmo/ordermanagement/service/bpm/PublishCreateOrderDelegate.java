package ru.itmo.ordermanagement.service.bpm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.itmo.ordermanagement.dto.CreateOrderRequest;
import ru.itmo.ordermanagement.dto.OrderItemDto;
import ru.itmo.ordermanagement.service.OrderDomainService;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PublishCreateOrderDelegate implements JavaDelegate {

    private final OrderDomainService orderDomainService;
    private final ObjectMapper objectMapper;

    @Value("${app.seller-reaction-timeout-minutes:10}")
    private int sellerTimeoutMinutes;

    @Value("${app.courier-arrival-timeout-minutes:30}")
    private int courierTimeoutMinutes;

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = BpmnVariables.optionalString(execution, "requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            execution.setVariable("requestId", requestId);
        }

        CreateOrderRequest request = new CreateOrderRequest();
        request.setRequestId(UUID.fromString(requestId));
        request.setCustomerId(BpmnVariables.requiredLong(execution, "customerId"));
        request.setSellerId(BpmnVariables.requiredLong(execution, "sellerId"));
        request.setItems(readItems(BpmnVariables.optionalString(execution, "itemsJson")));

        ensureTimeoutVariables(execution);
        orderDomainService.publishCreateOrderRequest(request);
    }

    private void ensureTimeoutVariables(DelegateExecution execution) {
        if (execution.getVariable("sellerTimeoutDuration") == null) {
            execution.setVariable("sellerTimeoutDuration", "PT" + sellerTimeoutMinutes + "M");
        }
        if (execution.getVariable("courierTimeoutDuration") == null) {
            execution.setVariable("courierTimeoutDuration", "PT" + courierTimeoutMinutes + "M");
        }
    }

    private List<OrderItemDto> readItems(String itemsJson) {
        if (itemsJson == null) {
            throw new IllegalArgumentException("Required BPMN variable is missing: itemsJson");
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid itemsJson BPMN variable", e);
        }
    }
}
