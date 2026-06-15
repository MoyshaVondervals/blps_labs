package ru.itmo.ordermanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.dto.CreateOrderRequest;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderProcessService {

    public static final String PROCESS_KEY = "orderProcess";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Value("${app.seller-reaction-timeout-minutes:10}")
    private int sellerTimeoutMinutes;

    @Value("${app.courier-arrival-timeout-minutes:30}")
    private int courierTimeoutMinutes;

    public String startOrderProcess(CreateOrderRequest request) {
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID());
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("requestId", request.getRequestId().toString());
        variables.put("customerId", request.getCustomerId());
        variables.put("sellerId", request.getSellerId());
        variables.put("itemsJson", writeItemsJson(request));
        variables.put("sellerTimeoutDuration", "PT" + sellerTimeoutMinutes + "M");
        variables.put("courierTimeoutDuration", "PT" + courierTimeoutMinutes + "M");

        return runtimeService
                .startProcessInstanceByKey(PROCESS_KEY, request.getRequestId().toString(), variables)
                .getProcessInstanceId();
    }

    public boolean hasActiveProcess(Long orderId) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .variableValueEquals("orderId", orderId)
                .active()
                .count() > 0;
    }

    public void completeOrderTask(Long orderId, String taskDefinitionKey, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey(taskDefinitionKey)
                .active()
                .singleResult();

        if (task == null) {
            throw new ResourceNotFoundException(
                    "Active Camunda task '" + taskDefinitionKey + "' not found for order #" + orderId);
        }

        taskService.complete(task.getId(), variables);
    }

    private String writeItemsJson(CreateOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getItems());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize order items", e);
        }
    }
}
