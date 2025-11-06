package com.aptech.aptechMall.entity;

/**
 * Enum representing payment gateway transaction status
 */
public enum GatewayTransactionStatus {
    PENDING,    // Transaction initiated, waiting for payment
    SUCCESS,    // Payment successful
    FAILED,     // Payment failed
    CANCELLED,  // Transaction cancelled by user
    EXPIRED     // Transaction expired (timeout)
}
