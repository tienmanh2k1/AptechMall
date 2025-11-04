package com.aptech.aptechMall.Exception;

/**
 * Exception thrown when an order is not found
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("Order not found with ID: " + orderId);
    }

    public OrderNotFoundException(String orderNumber, boolean byNumber) {
        super("Order not found with number: " + orderNumber);
    }
}
