package com.aptech.aptechMall.entity;

/**
 * Enum representing the lifecycle status of an order
 */
public enum OrderStatus {
    PENDING,      // Order created, awaiting payment/confirmation
    CONFIRMED,    // Order confirmed and being processed
    SHIPPING,     // Order shipped and in transit
    DELIVERED,    // Order successfully delivered
    CANCELLED     // Order cancelled by user or system
}
