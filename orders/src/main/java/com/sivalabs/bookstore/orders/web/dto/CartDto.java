package com.sivalabs.bookstore.orders.web.dto;

import java.math.BigDecimal;

/**
 * REST representation of the session cart including computed totals.
 */
public record CartDto(CartItemDto item, BigDecimal totalAmount) {}
