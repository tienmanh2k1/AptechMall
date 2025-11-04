package com.aptech.aptechMall.service;

import com.aptech.aptechMall.Exception.*;
import com.aptech.aptechMall.dto.order.CheckoutRequest;
import com.aptech.aptechMall.dto.order.OrderResponse;
import com.aptech.aptechMall.entity.*;
import com.aptech.aptechMall.repository.CartItemRepository;
import com.aptech.aptechMall.repository.CartRepository;
import com.aptech.aptechMall.repository.OrderRepository;
import com.aptech.aptechMall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Service for managing order operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    /**
     * Generate unique order number
     * Format: ORD-{timestamp}-{random}
     * Example: ORD-20231025143522-A3F9
     * @return Unique order number
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String randomPart = String.format("%04X", new Random().nextInt(0x10000));

        String orderNumber = "ORD-" + timestamp + "-" + randomPart;

        // Ensure uniqueness (very unlikely to collide, but check anyway)
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomPart = String.format("%04X", new Random().nextInt(0x10000));
            orderNumber = "ORD-" + timestamp + "-" + randomPart;
        }

        return orderNumber;
    }

    /**
     * Checkout - Create order from cart
     * @param userId User ID
     * @param request CheckoutRequest
     * @return OrderResponse DTO
     */
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        log.info("Processing checkout for user {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Get cart with items
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));

        // Validate cart not empty
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setTotalAmount(cart.calculateTotal());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());
        order.setNote(request.getNote());

        // Copy cart items to order items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.fromCartItem(cartItem);
            order.addItem(orderItem);
        }

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Created order {} with {} items, total: {}",
                savedOrder.getOrderNumber(),
                savedOrder.getItems().size(),
                savedOrder.getTotalAmount());

        // Clear cart after successful checkout
        cart.clearItems();
        cartItemRepository.deleteByCartId(cart.getId());
        cartRepository.save(cart);
        log.info("Cleared cart for user {}", userId);

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Get all orders for user with pagination
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of OrderResponse DTOs
     */
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        log.info("Getting orders for user {} with pagination", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        log.info("Found {} orders for user {}", orders.getTotalElements(), userId);

        return orders.map(OrderResponse::toSummary);
    }

    /**
     * Get order detail by ID
     * @param userId User ID (for security check)
     * @param orderId Order ID
     * @return OrderResponse DTO with items
     */
    public OrderResponse getOrderDetail(Long userId, Long orderId) {
        log.info("Getting order detail {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to access order {} belonging to user {}",
                    userId, orderId, order.getUserId());
            throw new OrderNotFoundException(orderId);
        }

        return OrderResponse.fromEntity(order);
    }

    /**
     * Update order status
     * @param userId User ID (for security check)
     * @param orderId Order ID
     * @param newStatus New status
     * @return OrderResponse DTO
     */
    public OrderResponse updateOrderStatus(Long userId, Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to {} for user {}", orderId, newStatus, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to update order {} belonging to user {}",
                    userId, orderId, order.getUserId());
            throw new OrderNotFoundException(orderId);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Updated order {} status to {}", orderId, newStatus);

        // Reload with items
        Order orderWithItems = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.fromEntity(orderWithItems);
    }

    /**
     * Cancel order (only if status is PENDING)
     * @param userId User ID (for security check)
     * @param orderId Order ID
     * @return OrderResponse DTO
     */
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to cancel order {} belonging to user {}",
                    userId, orderId, order.getUserId());
            throw new OrderNotFoundException(orderId);
        }

        // Check if order can be cancelled
        if (!order.isCancellable()) {
            throw new OrderNotCancellableException(order.getStatus());
        }

        order.cancel();
        Order cancelledOrder = orderRepository.save(order);

        log.info("Cancelled order {}", orderId);

        // Reload with items
        Order orderWithItems = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.fromEntity(orderWithItems);
    }

    /**
     * Get order by order number
     * @param userId User ID (for security check)
     * @param orderNumber Order number
     * @return OrderResponse DTO
     */
    public OrderResponse getOrderByNumber(Long userId, String orderNumber) {
        log.info("Getting order {} for user {}", orderNumber, userId);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber, true));

        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to access order {} belonging to user {}",
                    userId, orderNumber, order.getUserId());
            throw new OrderNotFoundException(orderNumber, true);
        }

        return OrderResponse.fromEntity(order);
    }
}
