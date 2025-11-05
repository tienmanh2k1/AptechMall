package com.aptech.aptechMall.entity;

/**
 * Enum representing overall payment status for an order
 * According to payment policy:
 * - Deposit: 70% of product cost only
 * - Remaining: 30% product cost + all fees (service, shipping, additional services)
 * - COD: Vietnam domestic shipping fee paid to delivery person (not from wallet)
 */
public enum OrderPaymentStatus {
    PENDING_DEPOSIT,    // Waiting for deposit payment (70% of product cost)
    DEPOSITED,          // Deposit received, order being processed in China
    PENDING_REMAINING,  // Goods arrived at China warehouse, waiting for remaining payment (30% + fees)
    WALLET_PAID,        // All wallet payments completed (70% + 30% + fees), ready to ship to Vietnam
    FULLY_COMPLETED     // Order completed, goods received by customer (including COD if applicable)
}
