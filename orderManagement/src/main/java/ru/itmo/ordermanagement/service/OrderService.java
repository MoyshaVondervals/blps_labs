package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.dto.CreateOrderRequest;
import ru.itmo.ordermanagement.dto.OrderResponse;
import ru.itmo.ordermanagement.dto.ReviewOrderRequest;
import ru.itmo.ordermanagement.model.enums.OrderStatus;

import java.util.Map;

import static ru.itmo.ordermanagement.security.Privilege.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDomainService orderDomainService;
    private final OrderProcessService orderProcessService;

    @PreAuthorize("hasAuthority('" + CREATE_ORDER + "')")
    public void createOrder(CreateOrderRequest request) {
        orderProcessService.startOrderProcess(request);
    }

    @PreAuthorize("hasAnyAuthority('" + CHANGE_ORDER_STATUS + "', '" + CANCEL_ORDER + "')")
    public OrderResponse reviewOrder(Long orderId, ReviewOrderRequest request) {
        if (orderProcessService.hasActiveProcess(orderId)) {
            orderProcessService.completeOrderTask(orderId, "reviewOrderTask", Map.of(
                    "canFulfill", request.isCanFulfill(),
                    "cancelReason", request.getCancelReason() == null ? "" : request.getCancelReason()
            ));
            return orderDomainService.getOrder(orderId);
        }
        return orderDomainService.reviewOrder(orderId, request);
    }

    @PreAuthorize("hasAuthority('" + CHANGE_ORDER_STATUS + "')")
    public OrderResponse assembleOrder(Long orderId) {
        if (orderProcessService.hasActiveProcess(orderId)) {
            orderProcessService.completeOrderTask(orderId, "assembleOrderTask", Map.of());
            return orderDomainService.getOrder(orderId);
        }
        return orderDomainService.assembleOrder(orderId);
    }

    @PreAuthorize("hasAnyAuthority('" + SEARCH_COURIER + "', '" + CHANGE_ORDER_STATUS + "')")
    public OrderResponse searchCourier(Long orderId) {
        if (orderProcessService.hasActiveProcess(orderId)) {
            orderProcessService.completeOrderTask(orderId, "searchCourierTask", Map.of());
            return orderDomainService.getOrder(orderId);
        }
        return orderDomainService.searchCourier(orderId);
    }

    @PreAuthorize("hasAuthority('" + ACCEPT_DELIVERY + "')")
    public OrderResponse courierAcceptDelivery(Long orderId, Long courierId) {
        if (orderProcessService.hasActiveProcess(orderId)) {
            orderProcessService.completeOrderTask(orderId, "acceptDeliveryTask", Map.of("courierId", courierId));
            return orderDomainService.getOrder(orderId);
        }
        return orderDomainService.courierAcceptDelivery(orderId, courierId);
    }

    @PreAuthorize("hasAuthority('" + GET_ORDER_TO_DELIVERY + "')")
    public OrderResponse courierArrived(Long orderId, Long courierId) {
        if (orderProcessService.hasActiveProcess(orderId)) {
            orderProcessService.completeOrderTask(orderId, "courierArrivedTask", Map.of("courierId", courierId));
            return orderDomainService.getOrder(orderId);
        }
        return orderDomainService.courierArrived(orderId, courierId);
    }

    @PreAuthorize("hasAuthority('" + DELIVER_ORDER + "')")
    public OrderResponse deliverOrder(Long orderId, Long courierId) {
        if (orderProcessService.hasActiveProcess(orderId)) {
            orderProcessService.completeOrderTask(orderId, "deliverOrderTask", Map.of("courierId", courierId));
            return orderDomainService.getOrder(orderId);
        }
        return orderDomainService.deliverOrder(orderId, courierId);
    }

    public OrderResponse getOrder(Long orderId) {
        return orderDomainService.getOrder(orderId);
    }

    @PreAuthorize("hasAuthority('" + VIEW_OWN_ORDERS + "')")
    public Page<OrderResponse> getOrdersByCustomer(Long customerId, Pageable pageable) {
        return orderDomainService.getOrdersByCustomer(customerId, pageable);
    }

    @PreAuthorize("hasAuthority('" + VIEW_ALL_ORDERS + "')")
    public Page<OrderResponse> getOrdersBySeller(Long sellerId, Pageable pageable) {
        return orderDomainService.getOrdersBySeller(sellerId, pageable);
    }

    @PreAuthorize("hasAuthority('" + VIEW_DELIVERY_ORDERS + "')")
    public Page<OrderResponse> getOrdersByCourier(Long courierId, Pageable pageable) {
        return orderDomainService.getOrdersByCourier(courierId, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderDomainService.getOrdersByStatus(status, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderDomainService.getAllOrders(pageable);
    }

    public void cancelOverdueOrders(int timeoutMinutes) {
        orderDomainService.cancelOverdueOrders(timeoutMinutes);
    }

    public void markDelayedOrders(int timeoutMinutes) {
        orderDomainService.markDelayedOrders(timeoutMinutes);
    }
}
