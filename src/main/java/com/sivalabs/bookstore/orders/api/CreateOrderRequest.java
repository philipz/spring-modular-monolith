package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Request to create a new order")
public record CreateOrderRequest(
        @Schema(description = "Customer information", required = true) @Valid Customer customer,
        @Schema(description = "Delivery address", example = "742 Evergreen Terrace, Springfield", required = true)
                @NotEmpty String deliveryAddress,
        @Schema(description = "Order item", required = true) @Valid OrderItem item) {}
