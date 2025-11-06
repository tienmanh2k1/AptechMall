package com.aptech.aptechMall.dto.systemfee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for system fee configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemFeeConfigResponse {

    private Long id;
    private BigDecimal serviceFeePercent;
    private BigDecimal domesticShippingRate;
    private BigDecimal internationalShippingRate;
    private BigDecimal vietnamDomesticShippingRate;
    private BigDecimal depositPercent;
    private boolean isActive;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
