package com.aptech.aptechMall.dto.order;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating order fees by admin/staff
 * Used to manually input shipping fees and additional services
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderFeesRequest {

    /**
     * Domestic shipping fee in China (CNY)
     * Admin/staff manually inputs this
     */
    @DecimalMin(value = "0.0", message = "Domestic shipping fee must be at least 0")
    private BigDecimal domesticShippingFee;

    /**
     * International shipping fee (China to Vietnam) in VND
     * Admin/staff manually inputs this
     */
    @DecimalMin(value = "0.0", message = "International shipping fee must be at least 0")
    private BigDecimal internationalShippingFee;

    /**
     * Estimated weight in kilograms
     * Used to calculate packaging fees
     */
    @DecimalMin(value = "0.0", message = "Weight must be at least 0")
    private BigDecimal estimatedWeight;

    /**
     * Include wooden packaging service
     */
    private Boolean includeWoodenPackaging;

    /**
     * Include bubble wrap packaging service
     */
    private Boolean includeBubbleWrap;

    /**
     * Include item count check service
     */
    private Boolean includeItemCountCheck;

    /**
     * Additional note from admin/staff
     */
    private String note;
}
