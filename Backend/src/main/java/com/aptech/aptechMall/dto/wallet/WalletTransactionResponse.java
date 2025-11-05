package com.aptech.aptechMall.dto.wallet;

import com.aptech.aptechMall.entity.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for wallet transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletTransactionResponse {
    private Long transactionId;
    private Long walletId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Long orderId; // Optional - for order-related transactions
    private String description;
    private String referenceNumber; // Payment gateway reference
    private Long performedBy; // Admin ID for manual adjustments
    private String note;
    private LocalDateTime createdAt;
    private boolean isCredit; // true if adds balance
    private boolean isDebit; // true if reduces balance
}
