package com.sivalabs.bookstore.orders.web.exception;

/**
 * Raised when a cart is not present in the current session context.
 */
public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String message) {
        super(message);
    }

    public static CartNotFoundException forSession(String sessionId) {
        return new CartNotFoundException("Cart not found for session: " + sessionId);
    }
}
