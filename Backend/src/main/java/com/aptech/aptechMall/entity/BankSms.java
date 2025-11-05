package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BankSms entity for storing bank transaction SMS notifications
 * Used to verify bank transfers and auto-process deposits
 */
@Entity
@Table(name = "bank_sms",
       indexes = {
           @Index(name = "idx_sms_processed", columnList = "processed"),
           @Index(name = "idx_sms_received_at", columnList = "received_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankSms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Phone number or sender ID of the bank
     */
    @Column(length = 50)
    private String sender;

    /**
     * SMS message content
     * Example: "TK 1234567890 +500,000 VND. GD: 123456789. ND: Nap tien USER123"
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Raw SMS data (complete SMS body)
     */
    @Column(columnDefinition = "LONGTEXT")
    private String raw;

    /**
     * When the SMS was received
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /**
     * Whether this SMS has been processed
     */
    @Column(nullable = false)
    private boolean processed = false;

    /**
     * When the SMS was processed
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Parsed amount from SMS (if successful)
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal parsedAmount;

    /**
     * Parsed transaction reference from SMS
     */
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    /**
     * User ID extracted from SMS content (if found)
     * Example: "ND: NAP TIEN USER123" -> userId = 123
     * @deprecated Use extractedEmail instead
     */
    @Column(name = "extracted_user_id")
    private Long extractedUserId;

    /**
     * Email extracted from SMS content (if found)
     * Example: "ND: NAP TIEN demo@gmail.com" -> email = demo@gmail.com
     * @deprecated Email contains special characters (@, .) not allowed in bank transfer notes
     */
    @Column(name = "extracted_email", length = 100)
    private String extractedEmail;

    /**
     * Username or deposit code extracted from SMS content (if found)
     * Example: "ND: NAP TIEN DEMOACCOUNT" -> username = DEMOACCOUNT
     * Format: Alphanumeric only (no special characters)
     */
    @Column(name = "extracted_username", length = 100)
    private String extractedUsername;

    /**
     * Whether deposit was successfully created from this SMS
     */
    @Column(name = "deposit_created")
    private boolean depositCreated = false;

    /**
     * Wallet transaction ID created from this SMS
     */
    @Column(name = "wallet_transaction_id")
    private Long walletTransactionId;

    /**
     * Error message if processing failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Mark SMS as processed
     */
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark deposit as created
     */
    public void markDepositCreated(Long transactionId) {
        this.depositCreated = true;
        this.walletTransactionId = transactionId;
    }

    /**
     * Set error message
     */
    public void setError(String error) {
        this.errorMessage = error;
    }
}
