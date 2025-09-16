package com.sivalabs.bookstore.orders;

public class OrderNotFoundException extends RuntimeException {

    private OrderNotFoundException(String message) {
        super(message);
    }

    public static OrderNotFoundException forOrderNumber(String orderNumber) {
        return new OrderNotFoundException("Order not found with orderNumber: " + orderNumber);
    }
}
