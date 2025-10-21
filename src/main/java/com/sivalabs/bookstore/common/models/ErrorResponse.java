package com.sivalabs.bookstore.common.models;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response for REST API")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "404", required = true) int status,
        @Schema(
                        description = "Error message describing what went wrong",
                        example = "Resource not found",
                        required = true)
                String message,
        @Schema(description = "Timestamp when the error occurred", example = "2025-10-18T10:30:00", required = true)
                LocalDateTime timestamp) {}
