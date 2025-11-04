package com.aptech.aptechMall.dto.cart;

import com.aptech.aptechMall.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO representing a cart in response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long id;
    private Long userId;
    private List<CartItemDTO> items;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Cart entity to DTO
     * @param cart Cart entity
     * @return CartResponse
     */
    public static CartResponse fromEntity(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(CartItemDTO::fromEntity)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemDTOs)
                .totalItems(cart.getTotalItems())
                .totalAmount(cart.calculateTotal())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    /**
     * Create empty cart response
     * @param userId User ID
     * @return Empty CartResponse
     */
    public static CartResponse empty(Long userId) {
        return CartResponse.builder()
                .userId(userId)
                .items(List.of())
                .totalItems(0)
                .totalAmount(BigDecimal.ZERO)
                .build();
    }
}
