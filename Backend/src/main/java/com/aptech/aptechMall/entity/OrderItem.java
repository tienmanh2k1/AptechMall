package com.aptech.aptechMall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * OrderItem entity representing individual products in an order
 * Many-to-One relationship with Order
 */
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

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

    /**
     * Calculate subtotal for this order item (price * quantity)
     * @return subtotal amount
     */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Create OrderItem from CartItem
     * @param cartItem CartItem to copy from
     * @return new OrderItem
     */
    public static OrderItem fromCartItem(CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(cartItem.getProductId());
        orderItem.setProductName(cartItem.getProductName());
        orderItem.setProductImage(cartItem.getProductImage());
        orderItem.setPrice(cartItem.getPrice());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setMarketplace(cartItem.getMarketplace());
        // Copy variant information
        orderItem.setVariantId(cartItem.getVariantId());
        orderItem.setVariantName(cartItem.getVariantName());
        orderItem.setVariantOptions(cartItem.getVariantOptions());
        return orderItem;
    }
}
