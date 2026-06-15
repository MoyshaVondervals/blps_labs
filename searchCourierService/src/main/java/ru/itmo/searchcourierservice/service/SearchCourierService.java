package ru.itmo.searchcourierservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.searchcourierservice.dto.CourierAssignedEvent;
import ru.itmo.searchcourierservice.dto.NotificationEvent;
import ru.itmo.searchcourierservice.dto.SearchCourierRequest;
import ru.itmo.searchcourierservice.exception.ResourceNotFoundException;
import ru.itmo.searchcourierservice.model.entity.Courier;
import ru.itmo.searchcourierservice.model.entity.Order;
import ru.itmo.searchcourierservice.model.enums.OrderStatus;
import ru.itmo.searchcourierservice.model.enums.RecipientType;
import ru.itmo.searchcourierservice.repository.CourierRepository;
import ru.itmo.searchcourierservice.repository.OrderRepository;
import ru.itmo.searchcourierservice.service.kafka.CourierAssignedEventPublisher;
import ru.itmo.searchcourierservice.service.outbox.OutboxEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchCourierService {

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final OutboxEventService outboxEventService;
    private final CourierAssignedEventPublisher courierAssignedEventPublisher;

    @Value("${topic.send-notification}")
    private String sendNotificationTopic;

    @Transactional(transactionManager = "jpaTransactionManager")
    public void searchCourier(SearchCourierRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + request.getOrderId()));

        if (order.getStatus() != OrderStatus.SEARCHING_COURIER) {
            log.info("Search courier request for order #{} ignored, current status: {}",
                    order.getId(), order.getStatus());
            return;
        }

        // Максимально простое назначение: первый свободный курьер, а если
        // свободных нет — просто первый курьер по id (чтобы заказ не зависал).
        Courier courier = courierRepository.findFirstByAvailableTrue()
                .or(courierRepository::findFirstByOrderByIdAsc)
                .orElse(null);
        if (courier == null) {
            log.warn("No couriers exist at all for order #{}", order.getId());
            return;
        }

        assignCourier(order, courier);
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
        courierAssignedEventPublisher.publish(new CourierAssignedEvent(order.getId(), courier.getId()));
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
                .eventId(UUID.randomUUID())
                .recipientType(recipientType)
                .recipientId(recipientId)
                .orderId(order.getId())
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        String key = recipientType + ":" + recipientId;
        outboxEventService.saveEvent("Order", order.getId(), "NotificationRequested",
                sendNotificationTopic, key, notification);
    }
}
