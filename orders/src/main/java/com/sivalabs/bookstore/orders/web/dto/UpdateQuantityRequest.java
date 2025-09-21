package com.sivalabs.bookstore.orders.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the quantity of the existing cart item.
 */
public record UpdateQuantityRequest(
        @NotNull(message = "Quantity is required") @Min(value = 0, message = "Quantity must be non-negative") Integer quantity) {}
