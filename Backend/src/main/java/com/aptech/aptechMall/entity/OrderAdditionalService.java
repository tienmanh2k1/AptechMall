package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderAdditionalService entity representing services applied to an order
 * Many-to-One relationship with Order
 * Many-to-One relationship with AdditionalService
 * Junction table with additional data (snapshot of service details at order time)
 */
@Entity
@Table(name = "order_additional_service",
       indexes = {
           @Index(name = "idx_order_service_order_id", columnList = "order_id"),
           @Index(name = "idx_order_service_service_id", columnList = "service_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private AdditionalService service;

    // Snapshot fields (store service details at time of order)
    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @NotNull(message = "Service fee is required")
    @DecimalMin(value = "0.0", message = "Service fee must be at least 0")
    @Column(name = "service_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "service_description", length = 1000)
    private String serviceDescription;

    @Column(columnDefinition = "TEXT")
    private String note; // Special instructions for this service

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Create OrderAdditionalService from AdditionalService
     * Snapshots the service details at order time
     * @param service AdditionalService to snapshot
     * @param orderValue Order value for percentage calculation
     * @return New OrderAdditionalService instance
     */
    public static OrderAdditionalService fromService(AdditionalService service, BigDecimal orderValue) {
        return OrderAdditionalService.builder()
                .service(service)
                .serviceName(service.getServiceName())
                .serviceFee(service.calculateFee(orderValue))
                .serviceDescription(service.getDescription())
                .build();
    }

    /**
     * Set order and link bidirectionally
     * @param order Order to link
     */
    public void setOrderBidirectional(Order order) {
        this.order = order;
        if (order != null && !order.getAdditionalServices().contains(this)) {
            order.getAdditionalServices().add(this);
        }
    }
}
