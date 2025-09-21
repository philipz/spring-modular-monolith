package com.sivalabs.bookstore.orders.web.dto;

import java.math.BigDecimal;

/**
 * Represents a single cart line item exposed via the REST API.
 */
public record CartItemDto(String code, String name, BigDecimal price, Integer quantity) {}
