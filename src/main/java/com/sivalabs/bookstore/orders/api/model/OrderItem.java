package com.sivalabs.bookstore.orders.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

public record OrderItem(
        @NotBlank(message = "Code is required") String code,
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Price is required") BigDecimal price,
        @NotNull @Min(value = 1, message = "Quantity must be greater than 0") Integer quantity)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
