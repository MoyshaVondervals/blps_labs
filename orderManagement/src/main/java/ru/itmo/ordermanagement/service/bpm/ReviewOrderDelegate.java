package ru.itmo.ordermanagement.service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.ordermanagement.dto.ReviewOrderRequest;
import ru.itmo.ordermanagement.service.OrderDomainService;

@Component
@RequiredArgsConstructor
public class ReviewOrderDelegate implements JavaDelegate {

    private final OrderDomainService orderDomainService;

    @Override
    public void execute(DelegateExecution execution) {
        ReviewOrderRequest request = new ReviewOrderRequest();
        request.setCanFulfill(BpmnVariables.requiredBoolean(execution, "canFulfill"));
        request.setCancelReason(BpmnVariables.optionalString(execution, "cancelReason"));

        orderDomainService.reviewOrder(BpmnVariables.requiredLong(execution, "orderId"), request);
    }
}
