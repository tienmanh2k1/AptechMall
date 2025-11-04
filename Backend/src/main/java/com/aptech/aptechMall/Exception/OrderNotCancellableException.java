package com.aptech.aptechMall.Exception;

import com.aptech.aptechMall.entity.OrderStatus;

/**
 * Exception thrown when attempting to cancel an order that cannot be cancelled
 */
public class OrderNotCancellableException extends RuntimeException {

    public OrderNotCancellableException(OrderStatus currentStatus) {
        super("Order cannot be cancelled. Current status: " + currentStatus + ". Only PENDING orders can be cancelled.");
    }

    public OrderNotCancellableException(String message) {
        super(message);
    }
}
