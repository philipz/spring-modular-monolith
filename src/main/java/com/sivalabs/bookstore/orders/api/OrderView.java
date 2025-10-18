package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary view of an order")
public record OrderView(
        @Schema(description = "Unique order number", example = "ORD-2025-001234", required = true) String orderNumber,
        @Schema(description = "Order status", example = "NEW", required = true) OrderStatus status,
        @Schema(description = "Customer information", required = true) Customer customer) {}
