package com.sivalabs.bookstore.orders.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Order line item representing a product in the order")
public record OrderItem(
        @Schema(description = "Product code", example = "P100", required = true) @NotBlank(message = "Code is required") String code,
        @Schema(description = "Product name", example = "The Hunger Games", required = true)
                @NotBlank(message = "Name is required") String name,
        @Schema(description = "Product price", example = "34.0", required = true)
                @NotNull(message = "Price is required") BigDecimal price,
        @Schema(description = "Order quantity", example = "2", required = true, minimum = "1")
                @NotNull @Min(value = 1, message = "Quantity must be greater than 0") Integer quantity)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
