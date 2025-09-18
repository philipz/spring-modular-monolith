package com.sivalabs.bookstore.orders.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after successfully creating an order")
public record CreateOrderResponse(
        @Schema(description = "Unique order number", example = "BK-1234567890") String orderNumber) {}
