package com.aptech.aptechMall.entity;

/**
 * Enum representing payment types for orders
 */
public enum PaymentType {
    DEPOSIT,    // Deposit payment (70% default)
    REMAINING,  // Remaining payment (30% default)
    FULL        // Full payment (100%)
}
