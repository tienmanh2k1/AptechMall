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

    // Fee breakdown fields
    @Column(name = "product_cost", precision = 10, scale = 2)
    private BigDecimal productCost;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "domestic_shipping_fee", precision = 10, scale = 2)
    private BigDecimal domesticShippingFee;

    @Column(name = "international_shipping_fee", precision = 10, scale = 2)
    private BigDecimal internationalShippingFee;

    @Column(name = "additional_services_fee", precision = 10, scale = 2)
    private BigDecimal additionalServicesFee; // Phí dịch vụ: đóng gỗ, bọt khí, kiểm đếm

    @Column(name = "estimated_weight", precision = 8, scale = 2)
    private BigDecimal estimatedWeight; // in kilograms

    @Column(name = "vietnam_domestic_shipping_fee", precision = 10, scale = 2)
    private BigDecimal vietnamDomesticShippingFee = BigDecimal.ZERO; // COD shipping fee in Vietnam

    @Column(name = "is_cod_shipping", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isCodShipping = true; // True if customer pays shipping to delivery person

    // Payment tracking fields
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "remaining_amount", precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private OrderPaymentStatus paymentStatus = OrderPaymentStatus.PENDING_DEPOSIT;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPayment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderAdditionalService> additionalServices = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

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

    /**
     * Add payment to order
     * @param payment OrderPayment to add
     */
    public void addPayment(OrderPayment payment) {
        payment.setOrder(this);
        payments.add(payment);
    }

    /**
     * Calculate total paid amount
     * @return sum of all completed payments
     */
    public BigDecimal getTotalPaidAmount() {
        return payments.stream()
                .filter(OrderPayment::isCompleted)
                .map(OrderPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if deposit is paid
     * @return true if deposit payment is completed
     */
    public boolean isDepositPaid() {
        return payments.stream()
                .anyMatch(p -> p.isDeposit() && p.isCompleted());
    }

    /**
     * Check if order wallet payment is complete
     * @return true if payment status is WALLET_PAID or FULLY_COMPLETED
     */
    public boolean isFullyPaid() {
        return this.paymentStatus == OrderPaymentStatus.WALLET_PAID
                || this.paymentStatus == OrderPaymentStatus.FULLY_COMPLETED;
    }

    /**
     * Update payment status based on payments
     */
    public void updatePaymentStatus() {
        if (isDepositPaid() && !isFullyPaid()) {
            this.paymentStatus = OrderPaymentStatus.DEPOSITED;
        }

        BigDecimal totalPaid = getTotalPaidAmount();
        if (totalPaid.compareTo(totalAmount) >= 0) {
            this.paymentStatus = OrderPaymentStatus.WALLET_PAID;
        }
    }

    /**
     * Add additional service to order
     * @param additionalService OrderAdditionalService to add
     */
    public void addAdditionalService(OrderAdditionalService additionalService) {
        additionalService.setOrder(this);
        additionalServices.add(additionalService);
    }

    /**
     * Calculate total additional services fee
     * @return sum of all additional services fees
     */
    public BigDecimal getTotalAdditionalServicesFee() {
        return additionalServices.stream()
                .map(OrderAdditionalService::getServiceFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Add status history entry
     * @param statusHistory OrderStatusHistory to add
     */
    public void addStatusHistory(OrderStatusHistory statusHistory) {
        statusHistory.setOrder(this);
        this.statusHistory.add(statusHistory);
    }

    /**
     * Change order status and record in history
     * @param newStatus New status
     * @param note Note for status change
     * @param changedBy User who changed (null for system)
     */
    public void changeStatus(OrderStatus newStatus, String note, Long changedBy) {
        OrderStatus previousStatus = this.status;
        this.status = newStatus;

        OrderStatusHistory history = OrderStatusHistory.create(
                this, newStatus, previousStatus, note, changedBy);
        addStatusHistory(history);
    }

    /**
     * Calculate deposit based on PRODUCT COST ONLY (70%)
     * According to policy: Deposit = 70% of product cost (không bao gồm phí)
     * @return Deposit amount (70% of product cost)
     */
    public BigDecimal calculateDeposit() {
        if (productCost == null) {
            return BigDecimal.ZERO;
        }
        return productCost.multiply(BigDecimal.valueOf(0.70))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate remaining amount (30% product + all fees EXCEPT VN domestic shipping)
     * According to policy: Remaining = 30% product + service + shipping fees + additional services
     * Does NOT include vietnamDomesticShippingFee (paid via COD)
     * @return Remaining amount to be paid from wallet
     */
    public BigDecimal calculateRemaining() {
        // 30% of product cost
        BigDecimal productRemaining = productCost != null
            ? productCost.multiply(BigDecimal.valueOf(0.30))
            : BigDecimal.ZERO;

        // Add all fees (except VN domestic shipping)
        BigDecimal serviceFeeAmount = serviceFee != null ? serviceFee : BigDecimal.ZERO;
        BigDecimal domesticFeeAmount = domesticShippingFee != null ? domesticShippingFee : BigDecimal.ZERO;
        BigDecimal intlFeeAmount = internationalShippingFee != null ? internationalShippingFee : BigDecimal.ZERO;
        BigDecimal additionalFee = additionalServicesFee != null ? additionalServicesFee : BigDecimal.ZERO;

        return productRemaining
                .add(serviceFeeAmount)
                .add(domesticFeeAmount)
                .add(intlFeeAmount)
                .add(additionalFee)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate total amount paid from wallet (deposit + remaining)
     * DOES NOT include Vietnam domestic shipping (COD)
     * @return Total wallet payment amount
     */
    public BigDecimal calculateTotalWalletPayment() {
        return calculateDeposit().add(calculateRemaining());
    }

    /**
     * Get grand total including COD shipping fee
     * @return Grand total (wallet payment + COD shipping if applicable)
     */
    public BigDecimal getGrandTotal() {
        BigDecimal walletTotal = calculateTotalWalletPayment();
        BigDecimal codFee = (isCodShipping && vietnamDomesticShippingFee != null)
            ? vietnamDomesticShippingFee
            : BigDecimal.ZERO;
        return walletTotal.add(codFee);
    }

    /**
     * Check if deposit matches calculated amount
     * @return true if deposit amount is set correctly
     */
    public boolean isDepositCorrect() {
        if (depositAmount == null) return false;
        return depositAmount.compareTo(calculateDeposit()) == 0;
    }

    /**
     * Check if remaining amount matches calculated amount
     * @return true if remaining amount is set correctly
     */
    public boolean isRemainingCorrect() {
        if (remainingAmount == null) return false;
        return remainingAmount.compareTo(calculateRemaining()) == 0;
    }

    /**
     * Update deposit and remaining amounts based on current product cost and fees
     */
    public void recalculatePaymentAmounts() {
        this.depositAmount = calculateDeposit();
        this.remainingAmount = calculateRemaining();
        this.totalAmount = calculateTotalWalletPayment();
    }
}
