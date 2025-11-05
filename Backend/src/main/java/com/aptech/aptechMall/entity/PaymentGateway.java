package com.aptech.aptechMall.entity;

/**
 * Enum representing payment gateway providers
 */
public enum PaymentGateway {
    VNPAY,          // VNPay
    MOMO,           // MoMo
    ZALOPAY,        // ZaloPay
    BANK_TRANSFER,  // Manual bank transfer
    COD,            // Cash on delivery
    ADMIN_MANUAL    // Admin manual adjustment
}
