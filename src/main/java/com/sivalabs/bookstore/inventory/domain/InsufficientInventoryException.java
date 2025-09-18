package com.sivalabs.bookstore.inventory.domain;

public class InsufficientInventoryException extends InventoryException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}
