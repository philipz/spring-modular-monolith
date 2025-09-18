package com.sivalabs.bookstore.inventory.domain;

public class InvalidInventoryAdjustmentException extends InventoryException {
    public InvalidInventoryAdjustmentException(String message) {
        super(message);
    }
}
