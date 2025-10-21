package com.sivalabs.bookstore.orders.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Complete order details")
public record OrderDto(
        @Schema(description = "Unique order number", example = "ORD-2025-001234", required = true) String orderNumber,
        @Schema(description = "Order item", required = true) OrderItem item,
        @Schema(description = "Customer information", required = true) Customer customer,
        @Schema(description = "Delivery address", example = "742 Evergreen Terrace, Springfield", required = true)
                String deliveryAddress,
        @Schema(description = "Order status", example = "NEW", required = true) OrderStatus status,
        @Schema(description = "Order creation timestamp", example = "2025-10-18T10:30:00", required = true)
                LocalDateTime createdAt) {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "Total order amount (price × quantity)",
            example = "68.0",
            accessMode = Schema.AccessMode.READ_ONLY)
    public BigDecimal getTotalAmount() {
        return item.price().multiply(BigDecimal.valueOf(item.quantity()));
    }
}
