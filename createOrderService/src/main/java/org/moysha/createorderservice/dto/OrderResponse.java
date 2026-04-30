package org.moysha.createorderservice.dto;

import lombok.Builder;
import lombok.Data;
import org.moysha.createorderservice.model.enums.OrderStatus;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long sellerId;
    private String sellerName;
    private Long courierId;
    private String courierName;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAt;
    private String cancelReason;
}
