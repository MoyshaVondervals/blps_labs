package ru.itmo.ordermanagement.service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.itmo.ordermanagement.service.OrderDomainService;

@Component
@RequiredArgsConstructor
public class CancelOrderBySellerTimeoutDelegate implements JavaDelegate {

    private final OrderDomainService orderDomainService;

    @Value("${app.seller-reaction-timeout-minutes:10}")
    private int sellerTimeoutMinutes;

    @Override
    public void execute(DelegateExecution execution) {
        orderDomainService.cancelOrderBySellerTimeout(
                BpmnVariables.requiredLong(execution, "orderId"),
                sellerTimeoutMinutes
        );
    }
}
