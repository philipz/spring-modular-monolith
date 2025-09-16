package com.sivalabs.bookstore.orders.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order status enumeration")
public enum OrderStatus {
    @Schema(description = "Order is newly created")
    NEW,

    @Schema(description = "Order is being processed")
    IN_PROCESS,

    @Schema(description = "Order has been delivered")
    DELIVERED,

    @Schema(description = "Order has been cancelled")
    CANCELLED,

    @Schema(description = "Order has encountered an error")
    ERROR
}
