package com.sivalabs.bookstore.orders.domain;

public class CatalogServiceException extends RuntimeException {

    public CatalogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
