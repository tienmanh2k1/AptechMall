package com.aptech.aptechMall.entity;

import com.aptech.aptechMall.model.jpa.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity representing a completed purchase order
 * Many-to-One relationship with User
 * One-to-Many relationship with OrderItem
 */
@Entity
@Table(name = "orders",
       indexes = {
           @Index(name = "idx_order_number", columnList = "orderNumber"),
           @Index(name = "idx_user_id", columnList = "userId"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "createdAt")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User user;

    @NotBlank(message = "Order number is required")
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String shippingAddress;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Column(nullable = false, length = 20)
    private String phone;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    @Column(length = 1000)
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Add item to order
     * @param item OrderItem to add
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /**
     * Check if order can be cancelled (only PENDING status)
     * @return true if cancellable
     */
    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * Cancel the order
     * @throws IllegalStateException if order is not in PENDING status
     */
    public void cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("Only pending orders can be cancelled. Current status: " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Get total number of items in order
     * @return total item count
     */
    public int getTotalItems() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
}
