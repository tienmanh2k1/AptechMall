package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CartItem entity representing individual products in a cart
 * Many-to-One relationship with Cart
 */
@Entity
@Table(name = "cart_items",
       indexes = {
           @Index(name = "idx_cart_product_variant", columnList = "cart_id, product_id, marketplace, variant_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @NotBlank(message = "Product ID is required")
    @Column(nullable = false)
    private String productId;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false, length = 500)
    private String productName;

    @Column(length = 1000)
    private String productImage;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Marketplace is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Marketplace marketplace;

    // Variant information (optional - for products with variants)
    @Column(length = 100)
    private String variantId;

    @Column(length = 200)
    private String variantName;

    @Column(length = 500)
    private String variantOptions; // JSON string of selected options

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate subtotal for this cart item (price * quantity)
     * @return subtotal amount
     */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Check if this item matches another product (for duplicate detection)
     * Includes variant check to differentiate between product variants
     * @param productId Product ID to compare
     * @param marketplace Marketplace to compare
     * @param variantId Variant ID to compare (can be null)
     * @return true if matches
     */
    public boolean isSameProduct(String productId, Marketplace marketplace, String variantId) {
        boolean productMatches = this.productId.equals(productId) && this.marketplace == marketplace;

        // If both variantIds are null, consider them the same
        if (variantId == null && this.variantId == null) {
            return productMatches;
        }

        // If one is null and the other is not, they're different
        if (variantId == null || this.variantId == null) {
            return false;
        }

        // Both are non-null, compare them
        return productMatches && this.variantId.equals(variantId);
    }
}
