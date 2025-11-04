package com.aptech.aptechMall.Exception;

/**
 * Exception thrown when a cart is not found
 */
public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(String message) {
        super(message);
    }

    public CartNotFoundException(Long userId) {
        super("Cart not found for user ID: " + userId);
    }
}
