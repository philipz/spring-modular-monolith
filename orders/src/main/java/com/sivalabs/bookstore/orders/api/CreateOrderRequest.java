package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Request to create a new order")
public record CreateOrderRequest(
        @Schema(description = "Customer information", required = true) @Valid Customer customer,
        @Schema(
                        description = "Delivery address for the order",
                        required = true,
                        example = "123 Main St, City, State 12345")
                @NotEmpty String deliveryAddress,
        @Schema(description = "Order item details", required = true) @Valid OrderItem item) {}
