package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * OrderStatusHistory entity for tracking order status changes
 * Many-to-One relationship with Order
 * Creates timeline of status transitions
 */
@Entity
@Table(name = "order_status_history",
       indexes = {
           @Index(name = "idx_status_history_order_id", columnList = "order_id"),
           @Index(name = "idx_status_history_status", columnList = "status"),
           @Index(name = "idx_status_history_created_at", columnList = "created_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "previous_status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus previousStatus;

    @Column(length = 1000)
    private String note; // Reason for status change or additional info

    @Column(name = "changed_by")
    private Long changedBy; // User ID of person who changed status (admin/system)

    @Column(name = "is_system_change", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isSystemChange = false; // True if changed by system automatically

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Create status history entry
     * @param order Order
     * @param newStatus New status
     * @param previousStatus Previous status
     * @param note Note
     * @param changedBy User who changed (null for system)
     * @return OrderStatusHistory instance
     */
    public static OrderStatusHistory create(Order order, OrderStatus newStatus,
                                           OrderStatus previousStatus, String note,
                                           Long changedBy) {
        return OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .previousStatus(previousStatus)
                .note(note)
                .changedBy(changedBy)
                .isSystemChange(changedBy == null)
                .build();
    }

    /**
     * Check if this is a system-initiated change
     * @return true if changed by system
     */
    public boolean isSystemChange() {
        return this.isSystemChange;
    }

    /**
     * Check if this is a manual change (by user or admin)
     * @return true if changed manually
     */
    public boolean isManualChange() {
        return !this.isSystemChange;
    }
}
