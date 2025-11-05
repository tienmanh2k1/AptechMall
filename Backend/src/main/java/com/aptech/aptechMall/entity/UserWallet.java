package com.aptech.aptechMall.entity;

import com.aptech.aptechMall.model.jpa.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UserWallet entity representing user's wallet balance
 * One-to-One relationship with User
 * One-to-Many relationship with WalletTransaction
 */
@Entity
@Table(name = "user_wallet",
       indexes = {
           @Index(name = "idx_wallet_user_id", columnList = "user_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_locked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isLocked = false;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WalletTransaction> transactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
    }

    /**
     * Add funds to wallet
     * @param amount Amount to add
     * @throws IllegalArgumentException if amount is negative or zero
     * @throws IllegalStateException if wallet is locked
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        if (isLocked) {
            throw new IllegalStateException("Wallet is locked");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Deduct funds from wallet
     * @param amount Amount to deduct
     * @throws IllegalArgumentException if amount is negative or zero
     * @throws IllegalStateException if wallet is locked or insufficient balance
     */
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (isLocked) {
            throw new IllegalStateException("Wallet is locked");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Check if wallet has sufficient balance
     * @param amount Amount to check
     * @return true if balance >= amount
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * Lock wallet (prevent transactions)
     */
    public void lock() {
        this.isLocked = true;
    }

    /**
     * Unlock wallet
     */
    public void unlock() {
        this.isLocked = false;
    }

    /**
     * Add transaction to history
     * @param transaction WalletTransaction
     */
    public void addTransaction(WalletTransaction transaction) {
        transaction.setWallet(this);
        this.transactions.add(transaction);
    }
}
