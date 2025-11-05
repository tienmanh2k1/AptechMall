package com.aptech.aptechMall.entity;

import com.aptech.aptechMall.model.jpa.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentGatewayLog entity for tracking payment gateway transactions
 * Logs all interactions with payment providers (VNPay, MoMo, ZaloPay, etc.)
 * Used for reconciliation and debugging
 */
@Entity
@Table(name = "payment_gateway_log",
       indexes = {
           @Index(name = "idx_gateway_log_user_id", columnList = "user_id"),
           @Index(name = "idx_gateway_log_wallet_transaction", columnList = "wallet_transaction_id"),
           @Index(name = "idx_gateway_log_transaction_id", columnList = "transaction_id"),
           @Index(name = "idx_gateway_log_status", columnList = "status"),
           @Index(name = "idx_gateway_log_gateway", columnList = "gateway")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;

    @NotNull(message = "Payment gateway is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGateway gateway;

    @NotBlank(message = "Transaction ID is required")
    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId; // Our internal transaction ID

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId; // Transaction ID from payment gateway

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GatewayTransactionStatus status = GatewayTransactionStatus.PENDING;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload; // JSON of request sent to gateway

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload; // JSON of response from gateway

    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload; // JSON of callback/webhook from gateway

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "ip_address", length = 50)
    private String ipAddress; // User's IP address

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl; // URL to redirect user for payment

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Mark transaction as successful
     * @param gatewayTransactionId Transaction ID from gateway
     * @param callbackPayload Callback data from gateway
     */
    public void markAsSuccess(String gatewayTransactionId, String callbackPayload) {
        this.status = GatewayTransactionStatus.SUCCESS;
        this.gatewayTransactionId = gatewayTransactionId;
        this.callbackPayload = callbackPayload;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark transaction as failed
     * @param errorMessage Error message
     * @param callbackPayload Callback data from gateway
     */
    public void markAsFailed(String errorMessage, String callbackPayload) {
        this.status = GatewayTransactionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.callbackPayload = callbackPayload;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark transaction as cancelled
     */
    public void markAsCancelled() {
        this.status = GatewayTransactionStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark transaction as expired
     */
    public void markAsExpired() {
        this.status = GatewayTransactionStatus.EXPIRED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Check if transaction is completed (success or failed)
     * @return true if status is SUCCESS or FAILED
     */
    public boolean isCompleted() {
        return status == GatewayTransactionStatus.SUCCESS
                || status == GatewayTransactionStatus.FAILED;
    }

    /**
     * Check if transaction is successful
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == GatewayTransactionStatus.SUCCESS;
    }
}
