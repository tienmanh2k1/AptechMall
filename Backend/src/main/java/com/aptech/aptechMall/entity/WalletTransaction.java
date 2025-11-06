package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WalletTransaction entity representing transaction history for user wallet
 * Many-to-One relationship with UserWallet
 * Optional Many-to-One relationship with Order (for order payments)
 */
@Entity
@Table(name = "wallet_transaction",
       indexes = {
           @Index(name = "idx_transaction_wallet_id", columnList = "wallet_id"),
           @Index(name = "idx_transaction_order_id", columnList = "order_id"),
           @Index(name = "idx_transaction_type", columnList = "transaction_type"),
           @Index(name = "idx_transaction_created_at", columnList = "created_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private UserWallet wallet;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Balance before is required")
    @Column(name = "balance_before", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceBefore;

    @NotNull(message = "Balance after is required")
    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber; // Transaction reference from payment gateway

    @Column(name = "performed_by")
    private Long performedBy; // Admin user ID (for admin adjustments)

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this is a credit transaction (adds balance)
     * @return true for DEPOSIT, ORDER_REFUND, ADMIN_ADJUSTMENT (positive)
     */
    public boolean isCredit() {
        return transactionType == TransactionType.DEPOSIT
                || transactionType == TransactionType.ORDER_REFUND
                || (transactionType == TransactionType.ADMIN_ADJUSTMENT
                    && balanceAfter.compareTo(balanceBefore) > 0);
    }

    /**
     * Check if this is a debit transaction (reduces balance)
     * @return true for WITHDRAWAL, ORDER_PAYMENT, ADMIN_ADJUSTMENT (negative)
     */
    public boolean isDebit() {
        return transactionType == TransactionType.WITHDRAWAL
                || transactionType == TransactionType.ORDER_PAYMENT
                || (transactionType == TransactionType.ADMIN_ADJUSTMENT
                    && balanceAfter.compareTo(balanceBefore) < 0);
    }

    /**
     * Check if transaction is related to an order
     * @return true if order is not null
     */
    public boolean isOrderRelated() {
        return this.order != null;
    }
}
