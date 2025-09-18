package com.sivalabs.bookstore.orders.infrastructure.catalog;

public class CatalogServiceException extends RuntimeException {

    public CatalogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
