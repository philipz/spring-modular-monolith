package com.sivalabs.bookstore.orders.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Request to update cart item quantity")
public record UpdateQuantityRequest(
        @Schema(description = "New quantity", required = true, example = "3", minimum = "1") @Min(1) int quantity) {}
