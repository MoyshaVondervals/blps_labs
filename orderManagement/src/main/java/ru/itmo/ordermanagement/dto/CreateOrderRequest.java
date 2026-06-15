package ru.itmo.ordermanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    private UUID requestId;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDto> items;
}
