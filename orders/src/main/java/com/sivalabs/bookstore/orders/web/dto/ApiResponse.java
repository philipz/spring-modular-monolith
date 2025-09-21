package com.sivalabs.bookstore.orders.web.dto;

import java.time.Instant;

/**
 * Generic envelope for REST responses to include metadata consistently.
 */
public record ApiResponse<T>(T data, String message, Instant timestamp) {

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message, Instant.now());
    }
}
