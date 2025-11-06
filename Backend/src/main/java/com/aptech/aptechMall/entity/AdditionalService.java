package com.aptech.aptechMall.entity;

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
 * AdditionalService entity representing optional services for orders
 * Examples: Quality check, wooden packaging, insurance, photo service
 * Managed by admin users
 */
@Entity
@Table(name = "additional_service",
       indexes = {
           @Index(name = "idx_service_type", columnList = "service_type"),
           @Index(name = "idx_service_is_active", columnList = "is_active")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Service type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    private ServiceType serviceType;

    @NotBlank(message = "Service name is required")
    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(length = 1000)
    private String description;

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0.0", message = "Fee must be at least 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "is_percentage", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isPercentage = false; // If true, fee is percentage of order value

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isActive = true;

    @Column(name = "display_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer displayOrder = 0; // For sorting services in UI

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
    }

    /**
     * Calculate service fee
     * @param orderValue Order total value (for percentage-based fees)
     * @return Service fee amount
     */
    public BigDecimal calculateFee(BigDecimal orderValue) {
        if (isPercentage) {
            return orderValue.multiply(fee)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        }
        return fee;
    }

    /**
     * Get display name with fee
     * @return Formatted string like "Quality Check - 20,000đ"
     */
    public String getDisplayNameWithFee() {
        if (isPercentage) {
            return String.format("%s - %s%%", serviceName, fee);
        }
        return String.format("%s - %sđ", serviceName, fee);
    }

    /**
     * Activate service
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate service
     */
    public void deactivate() {
        this.isActive = false;
    }
}
