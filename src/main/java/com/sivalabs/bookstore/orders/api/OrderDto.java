package com.sivalabs.bookstore.orders.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDto(
        String orderNumber,
        OrderItem item,
        Customer customer,
        String deliveryAddress,
        OrderStatus status,
        LocalDateTime createdAt) {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getTotalAmount() {
        return item.price().multiply(BigDecimal.valueOf(item.quantity()));
    }
}
