package ru.itmo.ordermanagement.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.itmo.ordermanagement.service.OrderService;

@Component
@Slf4j
public class CheckCourierTimeoutJob implements Job {

    private final OrderService orderService;

    @Value("${app.courier-arrival-timeout-minutes:30}")
    private int courierTimeoutMinutes;

    public CheckCourierTimeoutJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Checking for courier arrival timeout ({} min)...", courierTimeoutMinutes);
        orderService.markDelayedOrders(courierTimeoutMinutes);
    }
}
