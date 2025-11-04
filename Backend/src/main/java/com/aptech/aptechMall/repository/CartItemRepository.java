package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.CartItem;
import com.aptech.aptechMall.entity.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CartItem entity
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find cart item by cart ID, product ID, marketplace, and variant ID
     * Used for duplicate detection - includes variant to differentiate between product variants
     * @param cartId Cart ID
     * @param productId Product ID
     * @param marketplace Marketplace
     * @param variantId Variant ID (can be null)
     * @return Optional containing CartItem if found
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId " +
           "AND ci.productId = :productId " +
           "AND ci.marketplace = :marketplace " +
           "AND (:variantId IS NULL AND ci.variantId IS NULL OR ci.variantId = :variantId)")
    Optional<CartItem> findByCartIdAndProductIdAndMarketplaceAndVariant(
            @Param("cartId") Long cartId,
            @Param("productId") String productId,
            @Param("marketplace") Marketplace marketplace,
            @Param("variantId") String variantId
    );

    /**
     * Find all items for a specific cart
     * @param cartId Cart ID
     * @return List of CartItems
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Delete all items for a specific cart
     * @param cartId Cart ID
     */
    void deleteByCartId(Long cartId);

    /**
     * Count items in a cart
     * @param cartId Cart ID
     * @return Number of items
     */
    long countByCartId(Long cartId);
}
