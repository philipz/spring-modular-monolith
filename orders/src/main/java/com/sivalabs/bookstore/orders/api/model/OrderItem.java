package com.sivalabs.bookstore.orders.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Item details in an order")
public record OrderItem(
        @Schema(description = "Product code", example = "P100", required = true) @NotBlank(message = "Code is required") String code,
        @Schema(description = "Product name", example = "Spring Boot in Action", required = true)
                @NotBlank(message = "Name is required") String name,
        @Schema(description = "Product price", example = "29.99", required = true)
                @NotNull(message = "Price is required") BigDecimal price,
        @Schema(description = "Quantity to order", example = "2", minimum = "1", required = true) @NotNull @Min(1) Integer quantity)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
