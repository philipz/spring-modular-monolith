package com.sivalabs.bookstore.orders.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Schema(description = "Customer information for order")
public record Customer(
        @Schema(description = "Customer full name", example = "John Doe", required = true)
                @NotBlank(message = "Customer Name is required") String name,
        @Schema(description = "Customer email address", example = "john.doe@example.com", required = true)
                @NotBlank(message = "Customer email is required") @Email String email,
        @Schema(description = "Customer phone number", example = "+1-555-123-4567", required = true)
                @NotBlank(message = "Customer Phone number is required") String phone)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
