package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.dto.order.OrderResponse;
import com.aptech.aptechMall.dto.order.UpdateOrderFeesRequest;
import com.aptech.aptechMall.dto.order.UpdateOrderStatusRequest;
import com.aptech.aptechMall.entity.OrderStatus;
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

/**
 * REST Controller for admin order management
 * All endpoints require ADMIN or STAFF role
 * Base path: /api/admin/orders
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"})
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Get all orders (admin view) with pagination and filters
     * GET /api/admin/orders?page={page}&size={size}&status={status}&userId={userId}
     *
     * @param page Page number (default 0)
     * @param size Page size (default 10)
     * @param status Optional status filter
     * @param userId Optional user ID filter
     * @return Page of OrderResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId) {

        log.info("GET /api/admin/orders - page: {}, size: {}, status: {}, userId: {}",
                page, size, status, userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.getAllOrders(pageable, status, userId);

        return ResponseEntity.ok(
                ApiResponse.success(orders, "Orders retrieved successfully")
        );
    }

    /**
     * Get order detail by ID (admin view)
     * GET /api/admin/orders/{orderId}
     *
     * @param orderId Order ID
     * @return OrderResponse
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId) {

        log.info("GET /api/admin/orders/{}", orderId);

        OrderResponse order = orderService.getOrderByIdAdmin(orderId);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order retrieved successfully")
        );
    }

    /**
     * Update order status (admin operation)
     * PUT /api/admin/orders/{orderId}/status
     *
     * @param orderId Order ID
     * @param request UpdateOrderStatusRequest (new status, optional note)
     * @return Updated OrderResponse
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        log.info("PUT /api/admin/orders/{}/status - newStatus: {}, note: {}",
                orderId, request.getStatus(), request.getNote());

        OrderResponse order = orderService.updateOrderStatusAdmin(orderId, request);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order status updated successfully")
        );
    }

    /**
     * Update order fees (admin/staff operation)
     * PUT /api/admin/orders/{orderId}/fees
     *
     * @param orderId Order ID
     * @param request UpdateOrderFeesRequest (shipping fees, weight, additional services)
     * @return Updated OrderResponse
     */
    @PutMapping("/{orderId}/fees")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderFees(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderFeesRequest request) {

        log.info("PUT /api/admin/orders/{}/fees - domesticShipping: {}, internationalShipping: {}, weight: {}",
                orderId, request.getDomesticShippingFee(), request.getInternationalShippingFee(), request.getEstimatedWeight());

        OrderResponse order = orderService.updateOrderFees(orderId, request);

        return ResponseEntity.ok(
                ApiResponse.success(order, "Order fees updated successfully")
        );
    }
}
