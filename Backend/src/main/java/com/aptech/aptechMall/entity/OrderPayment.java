package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderPayment entity representing payment transactions for orders
 * Tracks deposits, remaining payments, and full payments
 * Many-to-One relationship with Order
 */
@Entity
@Table(name = "order_payment",
       indexes = {
           @Index(name = "idx_order_payment_order_id", columnList = "order_id"),
           @Index(name = "idx_order_payment_status", columnList = "payment_status"),
           @Index(name = "idx_order_payment_type", columnList = "payment_type")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Payment type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // BANK_TRANSFER, COD, MOMO, VNPAY, etc.

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef; // Reference number from payment gateway

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Mark payment as completed
     */
    public void markAsCompleted() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
    }

    /**
     * Mark payment as failed
     */
    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    /**
     * Check if this is a deposit payment
     * @return true if payment type is DEPOSIT
     */
    public boolean isDeposit() {
        return this.paymentType == PaymentType.DEPOSIT;
    }

    /**
     * Check if this is a remaining payment
     * @return true if payment type is REMAINING
     */
    public boolean isRemaining() {
        return this.paymentType == PaymentType.REMAINING;
    }

    /**
     * Check if payment is completed
     * @return true if payment status is COMPLETED
     */
    public boolean isCompleted() {
        return this.paymentStatus == PaymentStatus.COMPLETED;
    }
}
