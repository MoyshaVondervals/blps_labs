package ru.itmo.ordermanagement.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {

    private String name;

    private String description;

    @Min(value = 0, message = "Price must be non-negative")
    private BigDecimal price;

    private Boolean available;
}

