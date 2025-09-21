package com.sivalabs.bookstore.orders.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for adding an item to the cart.
 */
public record AddToCartRequest(@NotBlank(message = "Product code is required") String productCode) {}
