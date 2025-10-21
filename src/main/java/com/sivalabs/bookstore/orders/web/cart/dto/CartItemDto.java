package com.sivalabs.bookstore.orders.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Schema(description = "Shopping cart line item")
public record CartItemDto(
        @Schema(description = "Product code", example = "P001") String code,
        @Schema(description = "Product name", example = "Spring Boot in Action") String name,
        @Schema(description = "Unit price", example = "29.99") BigDecimal price,
        @Schema(description = "Quantity", example = "2", minimum = "1") @Min(1) int quantity,
        @Schema(description = "Subtotal (price * quantity)", example = "59.98") BigDecimal subtotal) {}
