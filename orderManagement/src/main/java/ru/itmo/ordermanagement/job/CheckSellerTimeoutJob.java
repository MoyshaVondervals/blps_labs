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
public class CheckSellerTimeoutJob implements Job {

    private final OrderService orderService;

    @Value("${app.seller-reaction-timeout-minutes:10}")
    private int sellerTimeoutMinutes;

    public CheckSellerTimeoutJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Checking for seller reaction timeout ({} min)...", sellerTimeoutMinutes);
        orderService.cancelOverdueOrders(sellerTimeoutMinutes);
    }
}
