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
 * Service quản lý giỏ hàng (Shopping Cart)
 *
 * Chức năng chính:
 * - Lấy giỏ hàng của user (tự động tạo nếu chưa có)
 * - Thêm sản phẩm vào giỏ hàng
 * - Cập nhật số lượng sản phẩm trong giỏ
 * - Xóa sản phẩm khỏi giỏ
 * - Xóa toàn bộ giỏ hàng
 *
 * QUAN HỆ DATABASE:
 * - Cart (1) ←→ (Many) CartItem
 * - Mỗi user chỉ có 1 Cart (1-1 relationship với User)
 * - Mỗi Cart có nhiều CartItem (danh sách sản phẩm)
 *
 * XỬ LÝ VARIANT:
 * - CartItem hỗ trợ product variants (màu sắc, size, v.v.)
 * - Cùng productId nhưng khác variant → tạo CartItem riêng
 * - Cùng productId và cùng variant → cộng thêm số lượng
 *
 * TRANSACTION:
 * - @Transactional: Tất cả methods trong class này chạy trong transaction
 * - Rollback tự động nếu có exception
 * - Đảm bảo data consistency
 *
 * BẢO MẬT:
 * - Luôn verify userId tồn tại trước khi thao tác
 * - Verify CartItem thuộc về đúng Cart của user (không cho xóa item của người khác)
 * - userId được lấy từ JWT token trong controller (đã authenticated)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional // Tất cả methods chạy trong transaction
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    /**
     * Lấy hoặc tạo mới giỏ hàng cho user
     *
     * Method này được dùng nội bộ để đảm bảo user luôn có giỏ hàng
     * Nếu user chưa có giỏ → tạo mới tự động
     *
     * @param userId User ID
     * @return Cart entity (tồn tại hoặc vừa mới tạo)
     * @throws UserNotFoundException nếu userId không tồn tại
     */
    private Cart getOrCreateCart(Long userId) {
        // Kiểm tra user có tồn tại không
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Tìm cart của user (kèm theo items để tránh N+1 query)
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    // Chưa có cart → tạo mới
                    log.info("Creating new cart for user: {}", userId);
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Lấy giỏ hàng của user với tất cả items
     *
     * Trả về CartResponse DTO chứa:
     * - Danh sách items (sản phẩm)
     * - Tổng số lượng items
     * - Tổng giá trị giỏ hàng
     *
     * Nếu user chưa có giỏ → trả về empty cart (không tạo mới)
     *
     * @param userId User ID
     * @return CartResponse DTO
     * @throws UserNotFoundException nếu userId không tồn tại
     */
    public CartResponse getCart(Long userId) {
        log.info("Getting cart for user: {}", userId);

        // Verify user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Optional<Cart> cartOpt = cartRepository.findByUserIdWithItems(userId);

        if (cartOpt.isEmpty()) {
            // User chưa có giỏ hàng → trả về empty cart
            log.info("No cart found for user {}, returning empty cart", userId);
            return CartResponse.empty(userId);
        }

        Cart cart = cartOpt.get();
        log.info("Found cart with {} items for user {}", cart.getItems().size(), userId);

        return CartResponse.fromEntity(cart);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     *
     * Logic xử lý:
     * 1. Lấy hoặc tạo cart cho user
     * 2. Kiểm tra sản phẩm (với variant) đã có trong giỏ chưa
     * 3. Nếu đã có → cộng thêm số lượng
     * 4. Nếu chưa có → tạo CartItem mới
     * 5. Trả về giỏ hàng đã cập nhật
     *
     * XỬ LÝ VARIANT:
     * - Cùng productId + cùng variantId → cộng thêm quantity
     * - Cùng productId + khác variantId → tạo CartItem riêng
     * - Ví dụ: iPhone 15 màu đen + iPhone 15 màu trắng = 2 CartItem khác nhau
     *
     * @param userId User ID
     * @param request Thông tin sản phẩm cần thêm
     * @return CartResponse với giỏ hàng đã cập nhật
     */
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        log.info("Adding product {} to cart for user {}", request.getProductId(), userId);

        Cart cart = getOrCreateCart(userId);

        // Tìm xem sản phẩm (với variant) đã có trong giỏ chưa
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductIdAndMarketplaceAndVariant(
                cart.getId(),
                request.getProductId(),
                request.getMarketplace(),
                request.getVariantId()
        );

        if (existingItem.isPresent()) {
            // Sản phẩm (với variant) đã có → cộng thêm số lượng
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
            log.info("Updated existing item quantity to {} for product {} (variant: {})",
                    newQuantity, request.getProductId(), request.getVariantId());
        } else {
            // Sản phẩm chưa có hoặc khác variant → tạo CartItem mới
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName(request.getProductName());
            newItem.setProductImage(request.getProductImage());
            newItem.setPrice(request.getPrice());
            newItem.setQuantity(request.getQuantity());
            newItem.setMarketplace(request.getMarketplace());

            // Set thông tin variant (nếu có)
            newItem.setVariantId(request.getVariantId());
            newItem.setVariantName(request.getVariantName());
            newItem.setVariantOptions(request.getVariantOptions());

            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            log.info("Added new item to cart: {} x{} (variant: {})",
                    request.getProductName(), request.getQuantity(), request.getVariantId());
        }

        // Refresh cart để lấy items mới nhất
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        return CartResponse.fromEntity(updatedCart);
    }

    /**
     * Cập nhật số lượng của một sản phẩm trong giỏ hàng
     *
     * BẢO MẬT:
     * - Verify CartItem thuộc về đúng Cart của user
     * - Không cho phép user sửa item của giỏ hàng người khác
     *
     * @param userId User ID
     * @param itemId Cart item ID cần cập nhật
     * @param newQuantity Số lượng mới
     * @return CartResponse với giỏ hàng đã cập nhật
     * @throws CartNotFoundException nếu user không có giỏ hàng
     * @throws CartItemNotFoundException nếu item không tồn tại
     * @throws IllegalArgumentException nếu item không thuộc về giỏ của user
     */
    public CartResponse updateItemQuantity(Long userId, Long itemId, Integer newQuantity) {
        log.info("Updating cart item {} quantity to {} for user {}", itemId, newQuantity, userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // BẢO MẬT: Verify item thuộc về cart của user (không cho sửa item của người khác)
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        log.info("Updated item {} quantity to {}", itemId, newQuantity);

        // Refresh cart để lấy tổng giá mới
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        return CartResponse.fromEntity(updatedCart);
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     *
     * BẢO MẬT:
     * - Verify CartItem thuộc về đúng Cart của user
     * - Không cho phép user xóa item của giỏ hàng người khác
     *
     * @param userId User ID
     * @param itemId Cart item ID cần xóa
     * @return CartResponse với giỏ hàng sau khi xóa
     * @throws CartNotFoundException nếu user không có giỏ hàng
     * @throws CartItemNotFoundException nếu item không tồn tại
     * @throws IllegalArgumentException nếu item không thuộc về giỏ của user
     */
    public CartResponse removeItem(Long userId, Long itemId) {
        log.info("Removing cart item {} for user {}", itemId, userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // BẢO MẬT: Verify item thuộc về cart của user
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        cart.removeItem(item); // Xóa khỏi collection trong Cart entity
        cartItemRepository.delete(item); // Xóa khỏi database

        log.info("Removed item {} from cart", itemId);

        // Refresh cart
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        return CartResponse.fromEntity(updatedCart);
    }

    /**
     * Xóa toàn bộ sản phẩm trong giỏ hàng
     *
     * Thường được gọi sau khi checkout thành công
     *
     * @param userId User ID
     * @return CartResponse rỗng (không còn item nào)
     * @throws CartNotFoundException nếu user không có giỏ hàng
     */
    public CartResponse clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        cart.clearItems(); // Xóa tất cả items khỏi collection
        cartItemRepository.deleteByCartId(cart.getId()); // Xóa tất cả items khỏi database
        cartRepository.save(cart);

        log.info("Cleared all items from cart for user {}", userId);

        return CartResponse.empty(userId);
    }
}
