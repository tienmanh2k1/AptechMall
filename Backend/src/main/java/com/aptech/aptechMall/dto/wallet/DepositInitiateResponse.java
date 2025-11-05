package com.aptech.aptechMall.dto.wallet;

import com.aptech.aptechMall.entity.PaymentGateway;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO when initiating a deposit
 * Contains payment gateway URL and transaction details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepositInitiateResponse {
    private Long transactionId; // Pending transaction ID
    private BigDecimal amount;
    private PaymentGateway paymentGateway;
    private String paymentUrl; // URL to redirect user to payment gateway
    private String transactionCode; // Unique code for this transaction
    private String message;
}
