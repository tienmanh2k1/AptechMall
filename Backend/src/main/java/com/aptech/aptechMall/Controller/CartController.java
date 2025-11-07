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
 * Controller quản lý giỏ hàng (Shopping Cart)
 *
 * Endpoint base: /api/cart
 * YÊU CẦU AUTHENTICATION: Tất cả endpoints cần JWT token
 *
 * BẢO MẬT QUAN TRỌNG:
 * - userId được lấy TỰ ĐỘNG từ JWT token (AuthenticationUtil.getCurrentUserId())
 * - KHÔNG BAO GIỜ chấp nhận userId từ client
 * - Mỗi user chỉ có thể truy cập giỏ hàng của chính mình
 *
 * Chức năng:
 * - Xem giỏ hàng (GET /api/cart)
 * - Thêm sản phẩm vào giỏ (POST /api/cart/items)
 * - Cập nhật số lượng (PUT /api/cart/items/{itemId})
 * - Xóa sản phẩm khỏi giỏ (DELETE /api/cart/items/{itemId})
 * - Xóa toàn bộ giỏ hàng (DELETE /api/cart/clear)
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j // Lombok tự động tạo logger
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"})
public class CartController {

    private final CartService cartService;

    /**
     * Lấy thông tin giỏ hàng của user hiện tại
     *
     * GET /api/cart
     *
     * Response bao gồm:
     * - Danh sách sản phẩm trong giỏ (items)
     * - Tổng số lượng sản phẩm (totalQuantity)
     * - Tổng giá trị (totalPrice)
     * - Thông tin cart (cartId, createdAt, updatedAt)
     *
     * @return CartResponse chứa toàn bộ thông tin giỏ hàng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        // Lấy userId từ JWT token (BẢO MẬT)
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/cart - userId: {}", userId);

        CartResponse cart = cartService.getCart(userId);

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Cart retrieved successfully")
        );
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     *
     * POST /api/cart/items
     *
     * Request body cần có:
     * - productId: ID sản phẩm từ marketplace (AliExpress/1688)
     * - quantity: Số lượng muốn thêm
     * - price: Giá sản phẩm tại thời điểm thêm
     * - productName, imageUrl, marketplace: Thông tin hiển thị
     *
     * Xử lý:
     * - Nếu sản phẩm đã có trong giỏ → Cộng thêm số lượng
     * - Nếu sản phẩm chưa có → Tạo CartItem mới
     *
     * @param request Thông tin sản phẩm cần thêm
     * @return CartResponse với giỏ hàng đã cập nhật
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
     * Cập nhật số lượng của một sản phẩm trong giỏ hàng
     *
     * PUT /api/cart/items/{itemId}
     *
     * Cho phép:
     * - Tăng số lượng (quantity > hiện tại)
     * - Giảm số lượng (quantity > 0)
     * - Nếu quantity = 0 → Xóa item khỏi giỏ
     *
     * Bảo mật:
     * - Kiểm tra itemId có thuộc về user hiện tại không
     * - Nếu không → throw exception
     *
     * @param itemId ID của CartItem cần cập nhật
     * @param request Chứa số lượng mới
     * @return CartResponse với giỏ hàng đã cập nhật
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
     * Xóa một sản phẩm khỏi giỏ hàng
     *
     * DELETE /api/cart/items/{itemId}
     *
     * Bảo mật:
     * - Kiểm tra itemId có thuộc về user hiện tại không
     * - Chỉ cho phép xóa item của chính mình
     *
     * @param itemId ID của CartItem cần xóa
     * @return CartResponse với giỏ hàng sau khi xóa
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
     * Xóa toàn bộ sản phẩm trong giỏ hàng
     *
     * DELETE /api/cart/clear
     *
     * Thường được sử dụng:
     * - Sau khi checkout thành công
     * - Khi user muốn làm mới giỏ hàng
     *
     * @return CartResponse rỗng (không còn item nào)
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
