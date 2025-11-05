package com.aptech.aptechMall.dto.wallet;

import com.aptech.aptechMall.entity.PaymentGateway;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for wallet deposit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum deposit amount is 1000 VND")
    private BigDecimal amount;

    @NotNull(message = "Payment gateway is required")
    private PaymentGateway paymentGateway;

    private String returnUrl; // URL to redirect after payment

    private String description; // Optional description
}
