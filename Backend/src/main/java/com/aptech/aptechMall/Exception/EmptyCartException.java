package com.aptech.aptechMall.Exception;

/**
 * Exception thrown when attempting to checkout with an empty cart
 */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Cannot checkout with an empty cart");
    }

    public EmptyCartException(String message) {
        super(message);
    }
}
