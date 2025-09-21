package com.sivalabs.bookstore.orders.web.exception;

/**
 * Raised when an unsupported or invalid cart operation is attempted.
 */
public class InvalidCartOperationException extends RuntimeException {
    public InvalidCartOperationException(String message) {
        super(message);
    }

    public static InvalidCartOperationException quantityOutOfRange(int quantity) {
        return new InvalidCartOperationException("Invalid cart quantity: " + quantity);
    }

    public static InvalidCartOperationException productMismatch(String pathProductCode, String currentProductCode) {
        return new InvalidCartOperationException(
                "Cart contains product " + currentProductCode + " but request targeted " + pathProductCode);
    }

    public static InvalidCartOperationException productUnavailable(String productCode) {
        return new InvalidCartOperationException("Product not available for code: " + productCode);
    }

    public static InvalidCartOperationException quantityNotProvided() {
        return new InvalidCartOperationException("Quantity must be provided for cart update");
    }
}
