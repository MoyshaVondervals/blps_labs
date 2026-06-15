package ru.itmo.ordermanagement.service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.itmo.ordermanagement.service.OrderDomainService;

@Component
@RequiredArgsConstructor
public class SearchCourierDelegate implements JavaDelegate {

    private final OrderDomainService orderDomainService;

    @Override
    public void execute(DelegateExecution execution) {
        orderDomainService.searchCourier(BpmnVariables.requiredLong(execution, "orderId"));
    }
}
