package com.aptech.aptechMall.Exception;

/**
 * Exception thrown when a cart item is not found
 */
public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(String message) {
        super(message);
    }

    public CartItemNotFoundException(Long itemId) {
        super("Cart item not found with ID: " + itemId);
    }
}
