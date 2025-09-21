package com.sivalabs.bookstore.web;

import com.sivalabs.bookstore.orders.api.model.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderForm(
        @Valid Customer customer, @NotEmpty(message = "Delivery address is required") String deliveryAddress) {}
