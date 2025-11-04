package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.dto.cart.AddToCartRequest;
import com.aptech.aptechMall.dto.cart.CartResponse;
import com.aptech.aptechMall.dto.cart.UpdateCartItemRequest;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for shopping cart operations
 * Base path: /api/cart
 *
 * SECURITY: All endpoints use authenticated user's ID from JWT token.
 * Users can only access their own cart.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"})
public class CartController {

    private final CartService cartService;

    /**
     * Get user's cart with all items
     * GET /api/cart
     *
     * @return CartResponse with items and total
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/cart - userId: {}", userId);

        CartResponse cart = cartService.getCart(userId);

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Cart retrieved successfully")
        );
    }

    /**
     * Add product to cart
     * POST /api/cart/items
     *
     * @param request AddToCartRequest (product details)
     * @return Updated CartResponse
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("POST /api/cart/items - userId: {}, product: {}", userId, request.getProductId());

        CartResponse cart = cartService.addToCart(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(cart, "Product added to cart successfully"));
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/items/{itemId}
     *
     * @param itemId Cart item ID
     * @param request UpdateCartItemRequest (new quantity)
     * @return Updated CartResponse
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("PUT /api/cart/items/{} - userId: {}, newQuantity: {}",
                itemId, userId, request.getQuantity());

        CartResponse cart = cartService.updateItemQuantity(userId, itemId, request.getQuantity());

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Cart item updated successfully")
        );
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/items/{itemId}
     *
     * @param itemId Cart item ID
     * @return Updated CartResponse
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable Long itemId) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("DELETE /api/cart/items/{} - userId: {}", itemId, userId);

        CartResponse cart = cartService.removeItem(userId, itemId);

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Item removed from cart successfully")
        );
    }

    /**
     * Clear all items from cart
     * DELETE /api/cart/clear
     *
     * @return Empty CartResponse
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("DELETE /api/cart/clear - userId: {}", userId);

        CartResponse cart = cartService.clearCart(userId);

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Cart cleared successfully")
        );
    }
}
