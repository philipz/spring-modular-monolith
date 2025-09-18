package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simplified order view for listing")
public record OrderView(
        @Schema(description = "Unique order number", example = "BK-1234567890") String orderNumber,
        @Schema(description = "Current status of the order") OrderStatus status,
        @Schema(description = "Customer information") Customer customer) {}
