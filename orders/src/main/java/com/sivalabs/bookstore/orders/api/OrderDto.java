package com.sivalabs.bookstore.orders.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Complete order information")
public record OrderDto(
        @Schema(description = "Unique order number", example = "BK-1234567890") String orderNumber,
        @Schema(description = "Order item details") OrderItem item,
        @Schema(description = "Customer information") Customer customer,
        @Schema(description = "Delivery address", example = "123 Main St, City, State 12345") String deliveryAddress,
        @Schema(description = "Current status of the order") OrderStatus status,
        @Schema(description = "Order creation timestamp") LocalDateTime createdAt) {

    @Schema(description = "Total amount for the order (calculated field)", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getTotalAmount() {
        return item.price().multiply(BigDecimal.valueOf(item.quantity()));
    }
}
