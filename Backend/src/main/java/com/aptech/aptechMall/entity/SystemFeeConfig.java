package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
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

/**
 * System fee configuration entity
 * Stores service fees, shipping rates, and deposit percentage
 * Managed by admin users
 */
@Entity
@Table(name = "system_fee_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemFeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Service fee percent is required")
    @DecimalMin(value = "0.0", message = "Service fee percent must be at least 0")
    @DecimalMax(value = "100.0", message = "Service fee percent must not exceed 100")
    @Column(name = "service_fee_percent", nullable = false, precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 5.00")
    private BigDecimal serviceFeePercent = BigDecimal.valueOf(5.00);

    @NotNull(message = "Domestic shipping rate is required")
    @DecimalMin(value = "0.0", message = "Domestic shipping rate must be at least 0")
    @Column(name = "domestic_shipping_rate", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal domesticShippingRate = BigDecimal.ZERO;

    @NotNull(message = "International shipping rate is required")
    @DecimalMin(value = "0.0", message = "International shipping rate must be at least 0")
    @Column(name = "international_shipping_rate", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal internationalShippingRate = BigDecimal.ZERO;

    @NotNull(message = "Vietnam domestic shipping rate is required")
    @DecimalMin(value = "0.0", message = "Vietnam domestic shipping rate must be at least 0")
    @Column(name = "vietnam_domestic_shipping_rate", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal vietnamDomesticShippingRate = BigDecimal.ZERO;

    @NotNull(message = "Deposit percent is required")
    @DecimalMin(value = "0.0", message = "Deposit percent must be at least 0")
    @DecimalMax(value = "100.0", message = "Deposit percent must not exceed 100")
    @Column(name = "deposit_percent", nullable = false, precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 70.00")
    private BigDecimal depositPercent = BigDecimal.valueOf(70.00);

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isActive = true;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.serviceFeePercent == null) {
            this.serviceFeePercent = BigDecimal.valueOf(5.00);
        }
        if (this.domesticShippingRate == null) {
            this.domesticShippingRate = BigDecimal.ZERO;
        }
        if (this.internationalShippingRate == null) {
            this.internationalShippingRate = BigDecimal.ZERO;
        }
        if (this.vietnamDomesticShippingRate == null) {
            this.vietnamDomesticShippingRate = BigDecimal.ZERO;
        }
        if (this.depositPercent == null) {
            this.depositPercent = BigDecimal.valueOf(70.00);
        }
    }

    /**
     * Calculate service fee based on product cost
     * @param productCost Total cost of products
     * @return Service fee amount
     */
    public BigDecimal calculateServiceFee(BigDecimal productCost) {
        return productCost.multiply(serviceFeePercent)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculate domestic shipping fee based on weight
     * @param weightKg Estimated weight in kilograms
     * @return Domestic shipping fee
     */
    public BigDecimal calculateDomesticShipping(BigDecimal weightKg) {
        return domesticShippingRate.multiply(weightKg);
    }

    /**
     * Calculate international shipping fee based on weight
     * @param weightKg Estimated weight in kilograms
     * @return International shipping fee
     */
    public BigDecimal calculateInternationalShipping(BigDecimal weightKg) {
        return internationalShippingRate.multiply(weightKg);
    }

    /**
     * Calculate Vietnam domestic shipping fee based on weight
     * @param weightKg Estimated weight in kilograms
     * @return Vietnam domestic shipping fee (COD)
     */
    public BigDecimal calculateVietnamDomesticShipping(BigDecimal weightKg) {
        return vietnamDomesticShippingRate.multiply(weightKg);
    }

    /**
     * Calculate deposit amount based on total
     * @param totalAmount Total order amount
     * @return Deposit amount (70% default)
     */
    public BigDecimal calculateDepositAmount(BigDecimal totalAmount) {
        return totalAmount.multiply(depositPercent)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculate remaining amount
     * @param totalAmount Total order amount
     * @return Remaining amount (30% default)
     */
    public BigDecimal calculateRemainingAmount(BigDecimal totalAmount) {
        BigDecimal depositAmount = calculateDepositAmount(totalAmount);
        return totalAmount.subtract(depositAmount);
    }
}
