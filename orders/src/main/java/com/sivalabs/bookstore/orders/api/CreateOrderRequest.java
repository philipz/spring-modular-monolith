package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(@Valid Customer customer, @NotEmpty String deliveryAddress, @Valid OrderItem item) {}
