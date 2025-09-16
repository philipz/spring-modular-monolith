package com.sivalabs.bookstore.orders.api.events;

import com.sivalabs.bookstore.orders.api.model.Customer;

public record OrderCreatedEvent(String orderNumber, String productCode, int quantity, Customer customer) {}
