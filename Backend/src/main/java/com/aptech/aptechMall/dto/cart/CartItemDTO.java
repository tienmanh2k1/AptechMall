package com.aptech.aptechMall.dto.cart;

import com.aptech.aptechMall.entity.CartItem;
import com.aptech.aptechMall.entity.Marketplace;
import com.aptech.aptechMall.util.CurrencyUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a cart item in response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDTO {

    private Long id;
    private String productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private Marketplace marketplace;
    private String currency; // USD or CNY based on marketplace
    private BigDecimal subtotal;
    private LocalDateTime createdAt;

    // Variant information
    private String variantId;
    private String variantName;
    private String variantOptions;

    /**
     * Convert CartItem entity to DTO
     * @param cartItem CartItem entity
     * @return CartItemDTO
     */
    public static CartItemDTO fromEntity(CartItem cartItem) {
        return CartItemDTO.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProductId())
                .productName(cartItem.getProductName())
                .productImage(cartItem.getProductImage())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .marketplace(cartItem.getMarketplace())
                .currency(CurrencyUtil.getCurrencyFromMarketplace(cartItem.getMarketplace()))
                .subtotal(cartItem.getSubtotal())
                .createdAt(cartItem.getCreatedAt())
                .variantId(cartItem.getVariantId())
                .variantName(cartItem.getVariantName())
                .variantOptions(cartItem.getVariantOptions())
                .build();
    }
}
