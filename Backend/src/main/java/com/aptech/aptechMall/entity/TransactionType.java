package com.aptech.aptechMall.entity;

/**
 * Enum representing wallet transaction types
 */
public enum TransactionType {
    DEPOSIT,            // User deposits money (adds balance)
    WITHDRAWAL,         // User withdraws money (reduces balance)
    ORDER_PAYMENT,      // Payment for order (reduces balance)
    ORDER_REFUND,       // Refund from cancelled order (adds balance)
    ADMIN_ADJUSTMENT    // Admin manually adjusts balance
}
