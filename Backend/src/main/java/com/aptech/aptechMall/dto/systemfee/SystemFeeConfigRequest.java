package com.aptech.aptechMall.dto.systemfee;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating/updating system fee configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemFeeConfigRequest {

    @NotNull(message = "Service fee percent is required")
    @DecimalMin(value = "0.0", message = "Service fee percent must be at least 0")
    @DecimalMax(value = "100.0", message = "Service fee percent must not exceed 100")
    private BigDecimal serviceFeePercent;

    @NotNull(message = "Domestic shipping rate is required")
    @DecimalMin(value = "0.0", message = "Domestic shipping rate must be at least 0")
    private BigDecimal domesticShippingRate;

    @NotNull(message = "International shipping rate is required")
    @DecimalMin(value = "0.0", message = "International shipping rate must be at least 0")
    private BigDecimal internationalShippingRate;

    @NotNull(message = "Vietnam domestic shipping rate is required")
    @DecimalMin(value = "0.0", message = "Vietnam domestic shipping rate must be at least 0")
    private BigDecimal vietnamDomesticShippingRate;

    @NotNull(message = "Deposit percent is required")
    @DecimalMin(value = "0.0", message = "Deposit percent must be at least 0")
    @DecimalMax(value = "100.0", message = "Deposit percent must not exceed 100")
    private BigDecimal depositPercent;

    private Boolean isActive;
}
