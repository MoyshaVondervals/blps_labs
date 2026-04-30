package org.moysha.createorderservice.service;

import lombok.RequiredArgsConstructor;
import org.moysha.createorderservice.dto.CreateOrderRequest;
import org.moysha.createorderservice.dto.NotificationEvent;
import org.moysha.createorderservice.dto.OrderItemDto;
import org.moysha.createorderservice.dto.OrderItemResponse;
import org.moysha.createorderservice.dto.OrderResponse;
import org.moysha.createorderservice.exception.InvalidOrderStateException;
import org.moysha.createorderservice.exception.ResourceNotFoundException;
import org.moysha.createorderservice.model.entity.*;
import org.moysha.createorderservice.model.enums.OrderStatus;
import org.moysha.createorderservice.model.enums.RecipientType;
import org.moysha.createorderservice.repository.*;
import org.moysha.createorderservice.service.kafka.NotificationEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateOrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final CourierRepository courierRepository;
    private final ProductRepository productRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Qualifier("chainedTransactionManager")
    private final PlatformTransactionManager txManager;
    //    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderResponse createOrder(CreateOrderRequest request) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        tx.setTimeout(30);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        return tx.execute(status -> {
            try {
                Customer customer = customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Customer not found: " + request.getCustomerId()));

                Seller seller = sellerRepository.findById(request.getSellerId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Seller not found: " + request.getSellerId()));
                Order order = Order.builder()
                        .customer(customer)
                        .seller(seller)
                        .status(OrderStatus.IN_PROCESSING)
                        .build();
                for (OrderItemDto itemDto : request.getItems()) {
                    Product product = productRepository.findById(itemDto.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found: " + itemDto.getProductId()));

                    if (!product.getSeller().getId().equals(seller.getId())) {
                        throw new InvalidOrderStateException(
                                "Product #" + product.getId() + " does not belong to seller #" + seller.getId());
                    }
                    if (!product.getAvailable()) {
                        throw new InvalidOrderStateException(
                                "Product '" + product.getName() + "' is not available");
                    }

                    OrderItem item = OrderItem.builder()
                            .product(product)
                            .productName(product.getName())
                            .quantity(itemDto.getQuantity())
                            .price(product.getPrice())
                            .build();
                    order.addItem(item);
                }
                order.recalculateTotal();
                order.setSellerNotifiedAt(LocalDateTime.now());

                order = orderRepository.save(order);

                notifySellerNewOrder(order);
                notifyCustomerStatusChanged(order);

                return toResponse(order);

            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    private void notifyCustomerStatusChanged(Order order) {
        String message = String.format("Изменён статус заказа #%d: \"%s\"",
                order.getId(), translateStatus(order.getStatus().name()));
        publishNotification(RecipientType.CUSTOMER, order.getCustomer().getId(), order, message);
    }

    private void notifySellerNewOrder(Order order) {
        String message = String.format("Новый заказ #%d от покупателя %s",
                order.getId(), order.getCustomer().getName());
        publishNotification(RecipientType.SELLER, order.getSeller().getId(), order, message);
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

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .sellerId(order.getSeller().getId())
                .sellerName(order.getSeller().getName())
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .courierName(order.getCourier() != null ? order.getCourier().getName() : null)
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream()
                        .map(i -> OrderItemResponse.builder()
                                .id(i.getId())
                                .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                                .productName(i.getProductName())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelReason(order.getCancelReason())
                .build();
    }
}
