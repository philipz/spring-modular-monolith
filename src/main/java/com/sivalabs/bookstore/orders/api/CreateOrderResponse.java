package com.sivalabs.bookstore.orders.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after creating an order")
public record CreateOrderResponse(
        @Schema(description = "Unique order number", example = "ORD-2025-001234", required = true)
                String orderNumber) {}
