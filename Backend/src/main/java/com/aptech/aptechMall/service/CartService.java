package com.aptech.aptechMall.service;

import com.aptech.aptechMall.Exception.CartItemNotFoundException;
import com.aptech.aptechMall.Exception.CartNotFoundException;
import com.aptech.aptechMall.Exception.UserNotFoundException;
import com.aptech.aptechMall.dto.cart.AddToCartRequest;
import com.aptech.aptechMall.dto.cart.CartResponse;
import com.aptech.aptechMall.entity.Cart;
import com.aptech.aptechMall.entity.CartItem;
import com.aptech.aptechMall.repository.CartItemRepository;
import com.aptech.aptechMall.repository.CartRepository;
import com.aptech.aptechMall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing shopping cart operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    /**
     * Get or create cart for user
     * @param userId User ID
     * @return Cart entity
     */
    private Cart getOrCreateCart(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    log.info("Creating new cart for user: {}", userId);
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Get user's cart with all items
     * @param userId User ID
     * @return CartResponse DTO
     */
    public CartResponse getCart(Long userId) {
        log.info("Getting cart for user: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Optional<Cart> cartOpt = cartRepository.findByUserIdWithItems(userId);

        if (cartOpt.isEmpty()) {
            log.info("No cart found for user {}, returning empty cart", userId);
            return CartResponse.empty(userId);
        }

        Cart cart = cartOpt.get();
        log.info("Found cart with {} items for user {}", cart.getItems().size(), userId);

        return CartResponse.fromEntity(cart);
    }

    /**
     * Add product to cart or update quantity if exists
     * @param userId User ID
     * @param request AddToCartRequest
     * @return CartResponse DTO
     */
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        log.info("Adding product {} to cart for user {}", request.getProductId(), userId);

        Cart cart = getOrCreateCart(userId);

        // Check if product with same variant already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductIdAndMarketplaceAndVariant(
                cart.getId(),
                request.getProductId(),
                request.getMarketplace(),
                request.getVariantId()
        );

        if (existingItem.isPresent()) {
            // Update quantity for existing item with same variant
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
            log.info("Updated existing item quantity to {} for product {} (variant: {})",
                    newQuantity, request.getProductId(), request.getVariantId());
        } else {
            // Add new item (different product or different variant)
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName(request.getProductName());
            newItem.setProductImage(request.getProductImage());
            newItem.setPrice(request.getPrice());
            newItem.setQuantity(request.getQuantity());
            newItem.setMarketplace(request.getMarketplace());

            // Set variant information if provided
            newItem.setVariantId(request.getVariantId());
            newItem.setVariantName(request.getVariantName());
            newItem.setVariantOptions(request.getVariantOptions());

            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            log.info("Added new item to cart: {} x{} (variant: {})",
                    request.getProductName(), request.getQuantity(), request.getVariantId());
        }

        // Refresh cart to get updated items
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        return CartResponse.fromEntity(updatedCart);
    }

    /**
     * Update cart item quantity
     * @param userId User ID
     * @param itemId Cart item ID
     * @param newQuantity New quantity
     * @return CartResponse DTO
     */
    public CartResponse updateItemQuantity(Long userId, Long itemId, Integer newQuantity) {
        log.info("Updating cart item {} quantity to {} for user {}", itemId, newQuantity, userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // Verify item belongs to user's cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        log.info("Updated item {} quantity to {}", itemId, newQuantity);

        // Refresh cart
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        return CartResponse.fromEntity(updatedCart);
    }

    /**
     * Remove item from cart
     * @param userId User ID
     * @param itemId Cart item ID
     * @return CartResponse DTO
     */
    public CartResponse removeItem(Long userId, Long itemId) {
        log.info("Removing cart item {} for user {}", itemId, userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // Verify item belongs to user's cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        log.info("Removed item {} from cart", itemId);

        // Refresh cart
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        return CartResponse.fromEntity(updatedCart);
    }

    /**
     * Clear all items from cart
     * @param userId User ID
     * @return CartResponse DTO (empty cart)
     */
    public CartResponse clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        cart.clearItems();
        cartItemRepository.deleteByCartId(cart.getId());
        cartRepository.save(cart);

        log.info("Cleared all items from cart for user {}", userId);

        return CartResponse.empty(userId);
    }
}
