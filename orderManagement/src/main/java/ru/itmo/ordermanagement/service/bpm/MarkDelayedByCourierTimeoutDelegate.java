package ru.itmo.ordermanagement.service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.itmo.ordermanagement.service.OrderDomainService;

@Component
@RequiredArgsConstructor
public class MarkDelayedByCourierTimeoutDelegate implements JavaDelegate {

    private final OrderDomainService orderDomainService;

    @Value("${app.courier-arrival-timeout-minutes:30}")
    private int courierTimeoutMinutes;

    @Override
    public void execute(DelegateExecution execution) {
        orderDomainService.markOrderDelayedByCourierTimeout(
                BpmnVariables.requiredLong(execution, "orderId"),
                courierTimeoutMinutes
        );
    }
}
