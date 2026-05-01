package ru.itmo.ordermanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private Long sellerId;
    private String sellerName;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

