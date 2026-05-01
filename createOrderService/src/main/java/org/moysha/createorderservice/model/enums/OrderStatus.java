package org.moysha.createorderservice.model.enums;

public enum OrderStatus {
    CREATED,
    IN_PROCESSING,
    COOKING,
    ASSEMBLING,
    SEARCHING_COURIER,
    AWAITING_COURIER,
    DELAYED,
    IN_DELIVERY,
    DELIVERED,
    CANCELLED
}
