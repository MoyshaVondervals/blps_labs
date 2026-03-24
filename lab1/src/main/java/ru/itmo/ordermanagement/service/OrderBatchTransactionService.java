package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.model.enums.OrderStatus;
import ru.itmo.ordermanagement.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBatchTransactionService {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void cancelOverdueBatch(List<Long> orderIds, int timeoutMinutes) {
        for (Long orderId : orderIds) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() != OrderStatus.IN_PROCESSING) {
                    return;
                }

                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelReason("Продавец не реагирует в течение " + timeoutMinutes + " минут");
                orderRepository.save(order);

                notificationService.notifyCustomerStatusChanged(order);
                log.warn("Order #{} auto-cancelled: seller timeout ({} min)", order.getId(), timeoutMinutes);
            });
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void markDelayedBatch(List<Long> orderIds, int timeoutMinutes) {
        for (Long orderId : orderIds) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() != OrderStatus.AWAITING_COURIER) {
                    return;
                }

                order.setStatus(OrderStatus.DELAYED);
                orderRepository.save(order);

                notificationService.notifyCustomerStatusChanged(order);
                log.warn("Order #{} marked as DELAYED: courier timeout ({} min)", order.getId(), timeoutMinutes);
            });
        }
    }
}
