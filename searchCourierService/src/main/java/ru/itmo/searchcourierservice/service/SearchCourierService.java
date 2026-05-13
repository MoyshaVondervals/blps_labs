package ru.itmo.searchcourierservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.searchcourierservice.dto.NotificationEvent;
import ru.itmo.searchcourierservice.dto.SearchCourierRequest;
import ru.itmo.searchcourierservice.exception.InvalidOrderStateException;
import ru.itmo.searchcourierservice.exception.ResourceNotFoundException;
import ru.itmo.searchcourierservice.model.entity.Courier;
import ru.itmo.searchcourierservice.model.entity.Order;
import ru.itmo.searchcourierservice.model.enums.OrderStatus;
import ru.itmo.searchcourierservice.model.enums.RecipientType;
import ru.itmo.searchcourierservice.repository.CourierRepository;
import ru.itmo.searchcourierservice.repository.OrderRepository;
import ru.itmo.searchcourierservice.service.kafka.NotificationEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchCourierService {

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Qualifier("chainedTransactionManager")
    private final PlatformTransactionManager txManager;

    public void searchCourier(SearchCourierRequest request) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        tx.setTimeout(30);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        tx.execute(status -> {
            try {
                Order order = orderRepository.findById(request.getOrderId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Order not found: " + request.getOrderId()));

                assertStatus(order, OrderStatus.SEARCHING_COURIER);

                Courier courier = courierRepository.findFirstByAvailableTrue().orElse(null);
                if (courier == null) {
                    log.info("No available couriers for order #{}", order.getId());
                    return null;
                }

                assignCourier(order, courier);
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    private void assignCourier(Order order, Courier courier) {
        courier.setAvailable(false);
        courierRepository.save(courier);

        order.setCourier(courier);
        order.setStatus(OrderStatus.AWAITING_COURIER);
        order.setCourierAssignedAt(LocalDateTime.now());
        order.setCourierNotifiedAt(LocalDateTime.now());
        orderRepository.save(order);

        notifyCourierNewDelivery(order);
        log.info("Order #{}: courier #{} assigned, status: AWAITING_COURIER",
                order.getId(), courier.getId());
    }

    private void notifyCourierNewDelivery(Order order) {
        String message = String.format("Уведомление о новом заказе #%d. Адрес заведения: %s",
                order.getId(), order.getSeller().getAddress());
        publishNotification(RecipientType.COURIER, order.getCourier().getId(), order, message);
    }

    private void publishNotification(RecipientType recipientType, Long recipientId, Order order, String message) {
        NotificationEvent notification = NotificationEvent.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .orderId(order.getId())
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationEventPublisher.publish(notification);
    }

    private void assertStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(
                    String.format("Order #%d has status %s, expected %s",
                            order.getId(), order.getStatus(), expected));
        }
    }
}
