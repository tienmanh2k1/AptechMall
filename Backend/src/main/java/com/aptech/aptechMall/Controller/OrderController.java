package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.dto.order.CheckoutRequest;
import com.aptech.aptechMall.dto.order.OrderResponse;
import com.aptech.aptechMall.dto.order.UpdateOrderAddressRequest;
import com.aptech.aptechMall.dto.order.UpdateOrderStatusRequest;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller quản lý đơn hàng (Orders)
 *
 * Endpoint base: /api/orders
 * YÊU CẦU AUTHENTICATION: Tất cả endpoints cần JWT token
 *
 * BẢO MẬT QUAN TRỌNG:
 * - userId lấy TỰ ĐỘNG từ JWT token
 * - KHÔNG chấp nhận userId từ client
 * - Mỗi user chỉ có thể truy cập đơn hàng của chính mình
 *
 * Chức năng:
 * - Checkout: Tạo đơn hàng từ giỏ hàng (POST /checkout)
 * - Xem danh sách đơn hàng (GET /)
 * - Xem chi tiết đơn hàng (GET /{orderId})
 * - Hủy đơn hàng (POST /{orderId}/cancel)
 * - Cập nhật địa chỉ giao hàng (PUT /{orderId}/address) - chỉ khi đơn PENDING
 *
 * Trạng thái đơn hàng:
 * - PENDING: Chờ xử lý
 * - PROCESSING: Đang xử lý
 * - SHIPPED: Đã gửi hàng
 * - DELIVERED: Đã giao hàng
 * - CANCELLED: Đã hủy
 *
 * Thanh toán:
 * - Sử dụng ví điện tử (wallet balance)
 * - Tự động trừ tiền khi checkout thành công
 * - Hoàn tiền khi hủy đơn (nếu đã thanh toán)
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"})
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng từ giỏ hàng (Checkout)
     *
     * POST /api/orders/checkout
     *
     * Quy trình:
     * 1. Lấy tất cả items từ giỏ hàng của user
     * 2. Kiểm tra giỏ hàng không rỗng
     * 3. Tính tổng tiền (bao gồm phí xử lý nếu có)
     * 4. Kiểm tra số dư ví đủ để thanh toán
     * 5. Trừ tiền từ ví user
     * 6. Tạo đơn hàng với trạng thái PENDING
     * 7. Xóa giỏ hàng sau khi tạo đơn thành công
     * 8. Ghi log giao dịch wallet
     *
     * Request body cần có:
     * - shippingAddress: Địa chỉ giao hàng
     * - recipientPhone: SĐT người nhận
     * - note: Ghi chú đơn hàng (optional)
     *
     * @param request Thông tin checkout
     * @return OrderResponse với đơn hàng vừa tạo
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("POST /api/orders/checkout - userId: {}", userId);

        OrderResponse order = orderService.checkout(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    /**
     * Lấy danh sách tất cả đơn hàng của user (có phân trang)
     *
     * GET /api/orders?page={page}&size={size}
     *
     * Response bao gồm:
     * - orders: Danh sách đơn hàng (OrderResponse - summary, không có items chi tiết)
     * - currentPage: Trang hiện tại
     * - totalItems: Tổng số đơn hàng
     * - totalPages: Tổng số trang
     * - pageSize: Số đơn hàng mỗi trang
     * - hasNext, hasPrevious: Có trang tiếp theo/trước không
     *
     * Mặc định:
     * - page = 0 (trang đầu tiên)
     * - size = 10 (10 đơn hàng mỗi trang)
     *
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng đơn hàng mỗi trang
     * @return Map chứa danh sách đơn hàng và metadata phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/orders - userId: {}, page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> ordersPage = orderService.getUserOrders(userId, pageable);

        // Tạo response với metadata phân trang
        Map<String, Object> response = new HashMap<>();
        response.put("orders", ordersPage.getContent());
        response.put("currentPage", ordersPage.getNumber());
        response.put("totalItems", ordersPage.getTotalElements());
        response.put("totalPages", ordersPage.getTotalPages());
        response.put("pageSize", ordersPage.getSize());
        response.put("hasNext", ordersPage.hasNext());
        response.put("hasPrevious", ordersPage.hasPrevious());

        return ResponseEntity.ok(
                ApiResponse.success(response, "Orders retrieved successfully")
        );
    }

    /**
     * Lấy thông tin chi tiết của một đơn hàng
     *
     * GET /api/orders/{orderId}
     *
     * Response bao gồm:
     * - Thông tin đơn hàng đầy đủ
     * - Danh sách items (sản phẩm) trong đơn
     * - Trạng thái đơn hàng
     * - Thông tin thanh toán
     * - Địa chỉ giao hàng
     *
     * Bảo mật: Chỉ cho phép xem đơn hàng của chính mình
     *
     * @param orderId ID của đơn hàng cần xem
     * @return OrderResponse với thông tin đầy đủ bao gồm items
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @PathVariable Long orderId) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/orders/{} - userId: {}", orderId, userId);

        OrderResponse order = orderService.getOrderDetail(userId, orderId);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order detail retrieved successfully")
        );
    }

    /**
     * Get order by order number
     * GET /api/orders/number/{orderNumber}
     *
     * @param orderNumber Order number
     * @return OrderResponse with items
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/orders/number/{} - userId: {}", orderNumber, userId);

        OrderResponse order = orderService.getOrderByNumber(userId, orderNumber);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order retrieved successfully")
        );
    }

    /**
     * Update order status (ADMIN ONLY)
     * PUT /api/orders/{orderId}/status
     *
     * @param orderId Order ID
     * @param request UpdateOrderStatusRequest (new status)
     * @return Updated OrderResponse
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("PUT /api/orders/{}/status - userId: {}, newStatus: {}",
                orderId, userId, request.getStatus());

        OrderResponse order = orderService.updateOrderStatus(userId, orderId, request.getStatus());

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order status updated successfully")
        );
    }

    /**
     * Cancel order (only if status is PENDING)
     * DELETE /api/orders/{orderId}
     *
     * @param orderId Order ID
     * @return Cancelled OrderResponse
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("DELETE /api/orders/{} - userId: {}", orderId, userId);

        OrderResponse order = orderService.cancelOrder(userId, orderId);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order cancelled successfully")
        );
    }

    /**
     * Pay remaining amount (30% + fees) from wallet
     * POST /api/orders/{orderId}/pay-remaining
     *
     * @param orderId Order ID
     * @return Updated OrderResponse
     */
    @PostMapping("/{orderId}/pay-remaining")
    public ResponseEntity<ApiResponse<OrderResponse>> payRemainingAmount(
            @PathVariable Long orderId) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("POST /api/orders/{}/pay-remaining - userId: {}", orderId, userId);

        OrderResponse order = orderService.payRemainingAmount(userId, orderId);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Remaining amount paid successfully")
        );
    }

    /**
     * Update order shipping address (User can only update when order is PENDING)
     * PUT /api/orders/{orderId}/address
     *
     * @param orderId Order ID
     * @param request UpdateOrderAddressRequest
     * @return Updated OrderResponse
     */
    @PutMapping("/{orderId}/address")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderAddress(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderAddressRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("PUT /api/orders/{}/address - userId: {}", orderId, userId);

        OrderResponse order = orderService.updateOrderAddress(userId, orderId, request);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Đã cập nhật địa chỉ đơn hàng thành công")
        );
    }
}
