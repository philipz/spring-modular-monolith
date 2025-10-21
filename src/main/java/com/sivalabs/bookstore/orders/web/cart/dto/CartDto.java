package com.sivalabs.bookstore.orders.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Shopping cart with items and total")
public record CartDto(
        @Schema(description = "Cart items", required = true) List<CartItemDto> items,
        @Schema(description = "Total amount", example = "99.99") BigDecimal totalAmount,
        @Schema(description = "Number of items in cart", example = "3") int itemCount) {}
