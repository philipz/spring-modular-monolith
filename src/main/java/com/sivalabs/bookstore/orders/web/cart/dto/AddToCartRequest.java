package com.sivalabs.bookstore.orders.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to add item to cart")
public record AddToCartRequest(
        @Schema(description = "Product code", required = true, example = "P001") @NotBlank String code,
        @Schema(description = "Quantity to add", required = true, example = "1", minimum = "1") @Min(1) int quantity) {}
