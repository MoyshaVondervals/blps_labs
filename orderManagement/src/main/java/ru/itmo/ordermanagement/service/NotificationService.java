package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.dto.NotificationEvent;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.Notification;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.model.enums.RecipientType;
import ru.itmo.ordermanagement.repository.NotificationRepository;
import ru.itmo.ordermanagement.service.kafka.NotificationEventPublisher;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public NotificationEvent send(RecipientType recipientType, Long recipientId,
                             Order order, String message) {
        NotificationEvent notification = NotificationEvent.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .orderId(order.getId())
                .message(message)
                .isRead(false)
                .build();
//        notification = notificationRepository.save(notification);

        notificationEventPublisher.publish(notification);

        return notification;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notifyCustomerStatusChanged(Order order) {
        String message = String.format("Изменён статус заказа #%d: \"%s\"",
                order.getId(), translateStatus(order.getStatus().name()));
        send(RecipientType.CUSTOMER, order.getCustomer().getId(), order, message);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notifySellerNewOrder(Order order) {
        String message = String.format("Новый заказ #%d от покупателя %s",
                order.getId(), order.getCustomer().getName());
        send(RecipientType.SELLER, order.getSeller().getId(), order, message);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notifyCourierNewDelivery(Order order) {
        String message = String.format("Уведомление о новом заказе #%d. Адрес заведения: %s",
                order.getId(), order.getSeller().getAddress());
        send(RecipientType.COURIER, order.getCourier().getId(), order, message);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notifySellerCourierAccepted(Order order) {
        String message = String.format("Курьер %s принял запрос на доставку заказа #%d",
                order.getCourier().getName(), order.getId());
        send(RecipientType.SELLER, order.getSeller().getId(), order, message);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notifySellerOrderDelivered(Order order) {
        String message = String.format("Заказ #%d доставлен клиенту %s",
                order.getId(), order.getCustomer().getName());
        send(RecipientType.SELLER, order.getSeller().getId(), order, message);
    }

    public Page<NotificationEvent> getNotifications(RecipientType recipientType, Long recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId, pageable)
                .map(this::toResponse);
    }

    public Page<NotificationEvent> getUnreadNotifications(RecipientType recipientType, Long recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientType, recipientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationEvent toResponse(Notification n) {
        return NotificationEvent.builder()
                .id(n.getId())
                .recipientType(n.getRecipientType())
                .recipientId(n.getRecipientId())
                .orderId(n.getOrder().getId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "CREATED" -> "Создан";
            case "IN_PROCESSING" -> "В обработке";
            case "COOKING" -> "Готовится";
            case "ASSEMBLING" -> "В сборке";
            case "SEARCHING_COURIER" -> "Поиск курьера";
            case "AWAITING_COURIER" -> "Ожидание курьера";
            case "DELAYED" -> "Задерживается";
            case "IN_DELIVERY" -> "В доставке";
            case "DELIVERED" -> "Доставлен";
            case "CANCELLED" -> "Отменён";
            default -> status;
        };
    }
}
