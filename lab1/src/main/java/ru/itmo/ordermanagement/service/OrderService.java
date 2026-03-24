package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.dto.*;
import ru.itmo.ordermanagement.exception.InvalidOrderStateException;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.*;
import ru.itmo.ordermanagement.model.enums.OrderStatus;
import ru.itmo.ordermanagement.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final CourierRepository courierRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderResponse createOrder(CreateOrderRequest request) {
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

        notificationService.notifySellerNewOrder(order);
        notificationService.notifyCustomerStatusChanged(order);

        log.info("Order #{} created, status: IN_PROCESSING", order.getId());
        return toResponse(order);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse reviewOrder(Long orderId, ReviewOrderRequest request) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.IN_PROCESSING);

        if (request.isCanFulfill()) {
            order.setStatus(OrderStatus.COOKING);
            order = orderRepository.save(order);
            notificationService.notifyCustomerStatusChanged(order);
            log.info("Order #{} accepted by seller, status: COOKING", orderId);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason(request.getCancelReason() != null
                    ? request.getCancelReason()
                    : "Продавец не может выполнить заказ");
            order = orderRepository.save(order);
            notificationService.notifyCustomerStatusChanged(order);
            log.info("Order #{} cancelled by seller: {}", orderId, order.getCancelReason());
        }

        return toResponse(order);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse assembleOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.COOKING);

        order.setStatus(OrderStatus.ASSEMBLING);
        order = orderRepository.save(order);

        notificationService.notifyCustomerStatusChanged(order);
        log.info("Order #{} assembled, status: ASSEMBLING", orderId);
        return toResponse(order);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderResponse searchCourier(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.ASSEMBLING);

        order.setStatus(OrderStatus.SEARCHING_COURIER);
        order = orderRepository.save(order);

        final Order savedOrder = order;
        courierRepository.findFirstByAvailableTrue().ifPresent(courier ->
            assignCourier(savedOrder, courier)
        );

        return toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void assignCourier(Order order, Courier courier) {
        courier.setAvailable(false);
        courierRepository.save(courier);

        order.setCourier(courier);
        order.setStatus(OrderStatus.AWAITING_COURIER);
        order.setCourierAssignedAt(LocalDateTime.now());
        order.setCourierNotifiedAt(LocalDateTime.now());
        orderRepository.save(order);

        notificationService.notifyCourierNewDelivery(order);
        log.info("Order #{}: courier #{} assigned, status: AWAITING_COURIER",
                order.getId(), courier.getId());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse courierAcceptDelivery(Long orderId, Long courierId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.AWAITING_COURIER);

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new InvalidOrderStateException(
                    "Courier #" + courierId + " is not assigned to order #" + orderId);
        }

        notificationService.notifySellerCourierAccepted(order);
        notificationService.notifyCustomerStatusChanged(order);
        log.info("Order #{}: courier #{} accepted delivery request", orderId, courierId);
        return toResponse(order);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse courierArrived(Long orderId, Long courierId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.AWAITING_COURIER
                && order.getStatus() != OrderStatus.DELAYED) {
            throw new InvalidOrderStateException(
                    "Order #" + orderId + " is not in AWAITING_COURIER or DELAYED status");
        }

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new InvalidOrderStateException(
                    "Courier #" + courierId + " is not assigned to order #" + orderId);
        }

        order.setCourierArrivedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.IN_DELIVERY);
        order = orderRepository.save(order);

        notificationService.notifyCustomerStatusChanged(order);
        log.info("Order #{}: courier arrived, status: IN_DELIVERY", orderId);
        return toResponse(order);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse deliverOrder(Long orderId, Long courierId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.IN_DELIVERY);

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new InvalidOrderStateException(
                    "Courier #" + courierId + " is not assigned to order #" + orderId);
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order = orderRepository.save(order);

        Courier courier = order.getCourier();
        courier.setAvailable(true);
        courierRepository.save(courier);

        notificationService.notifyCustomerStatusChanged(order);
        notificationService.notifySellerOrderDelivered(order);
        log.info("Order #{}: delivered by courier #{}, status: DELIVERED", orderId, courierId);
        return toResponse(order);
    }

    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrderOrThrow(orderId));
    }

    public Page<OrderResponse> getOrdersByCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }

    public Page<OrderResponse> getOrdersBySeller(Long sellerId, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, pageable)
                .map(this::toResponse);
    }

    public Page<OrderResponse> getOrdersByCourier(Long courierId, Pageable pageable) {
        return orderRepository.findByCourierId(courierId, pageable)
                .map(this::toResponse);
    }

    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelOverdueOrders(int timeoutMinutes) {

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        Pageable batch = PageRequest.of(0, 100, Sort.by("id").ascending());
        while (true){
            Page<Order> overdueOrders = orderRepository
                    .findByStatusAndSellerNotifiedAtBefore(OrderStatus.IN_PROCESSING, deadline, batch);
            if (overdueOrders.isEmpty()) {
                break;
            }
            for (Order order : overdueOrders) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelReason("Продавец не реагирует в течение " + timeoutMinutes + " минут");
                orderRepository.save(order);

                notificationService.notifyCustomerStatusChanged(order);
                log.warn("Order #{} auto-cancelled: seller timeout ({} min)", order.getId(), timeoutMinutes);
            }


        }



    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void markDelayedOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        Pageable batch = PageRequest.of(0, 100, Sort.by("id").ascending());
        while (true){
            Page<Order> delayedOrders = orderRepository
                    .findByStatusAndCourierAssignedAtBefore(OrderStatus.AWAITING_COURIER, deadline, batch);
            if (delayedOrders.isEmpty()) {
                break;
            }

            for (Order order : delayedOrders) {
                order.setStatus(OrderStatus.DELAYED);
                orderRepository.save(order);

                notificationService.notifyCustomerStatusChanged(order);
                log.warn("Order #{} marked as DELAYED: courier timeout ({} min)", order.getId(), timeoutMinutes);
            }

        }

    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void assertStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(
                    String.format("Order #%d has status %s, expected %s",
                            order.getId(), order.getStatus(), expected));
        }
    }

    public OrderResponse toResponse(Order order) {
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
