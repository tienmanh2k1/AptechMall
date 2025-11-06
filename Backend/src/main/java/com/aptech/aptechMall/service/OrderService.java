package com.aptech.aptechMall.service;

import com.aptech.aptechMall.Exception.*;
import com.aptech.aptechMall.dto.exchange.ExchangeRateResponse;
import com.aptech.aptechMall.dto.order.CheckoutRequest;
import com.aptech.aptechMall.dto.order.OrderResponse;
import com.aptech.aptechMall.dto.order.UpdateOrderStatusRequest;
import com.aptech.aptechMall.entity.*;
import com.aptech.aptechMall.repository.CartItemRepository;
import com.aptech.aptechMall.repository.CartRepository;
import com.aptech.aptechMall.repository.OrderRepository;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.repository.UserWalletRepository;
import com.aptech.aptechMall.repository.WalletTransactionRepository;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.service.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final WalletService walletService;
    private final ExchangeRateService exchangeRateService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserWalletRepository userWalletRepository;

    /**
     * Infer currency from marketplace
     * @param marketplace Marketplace enum
     * @return Currency code (USD for ALIEXPRESS, CNY for ALIBABA1688)
     */
    private String inferCurrency(Marketplace marketplace) {
        if (marketplace == null) {
            return "USD"; // Default to USD
        }

        if (marketplace == Marketplace.ALIEXPRESS) {
            return "USD";
        } else if (marketplace == Marketplace.ALIBABA1688) {
            return "CNY";
        } else {
            return "USD";
        }
    }

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

        // Filter cart items by provided IDs (if any)
        var itemsToCheckout = cart.getItems();
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            itemsToCheckout = cart.getItems().stream()
                    .filter(item -> request.getItemIds().contains(item.getId()))
                    .toList();

            log.info("Filtering cart items: {} selected out of {} total",
                    itemsToCheckout.size(), cart.getItems().size());

            // Validate at least one item was selected
            if (itemsToCheckout.isEmpty()) {
                throw new EmptyCartException();
            }
        }

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());
        order.setNote(request.getNote());

        // Copy selected cart items to order items and calculate total in VND
        BigDecimal totalVND = BigDecimal.ZERO;
        for (CartItem cartItem : itemsToCheckout) {
            // Validate marketplace exists
            if (cartItem.getMarketplace() == null) {
                log.error("Cart item {} has null marketplace", cartItem.getId());
                throw new IllegalStateException(
                    "Invalid cart item: missing marketplace information. " +
                    "Please remove item '" + cartItem.getProductName() + "' and re-add it to your cart."
                );
            }

            OrderItem orderItem = OrderItem.fromCartItem(cartItem);
            order.addItem(orderItem);

            // Convert item total to VND using exchange rate
            BigDecimal itemTotal = cartItem.getSubtotal();
            String currency = inferCurrency(cartItem.getMarketplace());

            ExchangeRateResponse exchangeRate = exchangeRateService.getRate(currency);

            // Validate exchange rate value
            if (exchangeRate.getRateToVnd() == null ||
                exchangeRate.getRateToVnd().compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Invalid exchange rate for {}: {}", currency, exchangeRate.getRateToVnd());
                throw new IllegalStateException(
                    "Invalid exchange rate for " + currency + ". Please try again later."
                );
            }

            BigDecimal itemTotalVND = itemTotal.multiply(exchangeRate.getRateToVnd())
                    .setScale(0, java.math.RoundingMode.HALF_UP);

            log.debug("Item {}: {} {} x {} = {} VND",
                    cartItem.getProductName(),
                    itemTotal, currency,
                    exchangeRate.getRateToVnd(),
                    itemTotalVND);

            totalVND = totalVND.add(itemTotalVND);
        }

        // Set product cost in VND
        order.setProductCost(totalVND);

        // Calculate deposit (70% of product cost)
        BigDecimal depositAmount = totalVND.multiply(BigDecimal.valueOf(0.70))
                .setScale(0, java.math.RoundingMode.HALF_UP);
        order.setDepositAmount(depositAmount);

        // Calculate remaining (30% of product cost)
        BigDecimal remainingAmount = totalVND.subtract(depositAmount);
        order.setRemainingAmount(remainingAmount);

        // Set totalAmount (for wallet payment, this is same as deposit for now)
        order.setTotalAmount(depositAmount);

        log.info("Order amounts - Product: {} VND, Deposit: {} VND, Remaining: {} VND",
                totalVND, depositAmount, remainingAmount);

        // Get wallet and check balance
        UserWallet wallet = walletService.getOrCreateWallet(userId);

        if (wallet.isLocked()) {
            throw new IllegalStateException("Your wallet is locked. Please contact support.");
        }

        if (!wallet.hasSufficientBalance(depositAmount)) {
            BigDecimal shortfall = depositAmount.subtract(wallet.getBalance());
            throw new IllegalStateException(
                String.format("Insufficient wallet balance. You need %.0f VND more. Current balance: %.0f VND, Required deposit: %.0f VND",
                    shortfall, wallet.getBalance(), depositAmount));
        }

        // Record balance before and after (calculate new balance)
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(depositAmount);

        // Save order first to get ID (if this fails, nothing is committed)
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} saved with ID {}", savedOrder.getOrderNumber(), savedOrder.getId());

        // Create wallet transaction record (linked to order)
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(TransactionType.ORDER_PAYMENT)
                .amount(depositAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .order(savedOrder)
                .description(String.format("Order deposit payment for order #%s", savedOrder.getOrderNumber()))
                .build();

        walletTransactionRepository.save(transaction);

        // NOW update wallet balance and save (last step, atomic with transaction)
        // If this fails, entire transaction will rollback including order and transaction record
        wallet.setBalance(balanceAfter);
        userWalletRepository.save(wallet);
        log.info("Wallet balance updated: {} VND -> {} VND", balanceBefore, balanceAfter);
        log.info("Created order {} with {} items, total: {}",
                savedOrder.getOrderNumber(),
                savedOrder.getItems().size(),
                savedOrder.getTotalAmount());

        // Delete only checked-out items from cart
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            // Delete specific items
            for (CartItem item : itemsToCheckout) {
                cart.getItems().remove(item);
                cartItemRepository.delete(item);
            }
            cartRepository.save(cart);
            log.info("Removed {} items from cart for user {}", itemsToCheckout.size(), userId);
        } else {
            // Clear entire cart (backward compatibility)
            cart.clearItems();
            cartItemRepository.deleteByCartId(cart.getId());
            cartRepository.save(cart);
            log.info("Cleared entire cart for user {}", userId);
        }

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

        // Process refund if deposit was paid
        if (order.getDepositAmount() != null && order.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

            // Record balance before refund
            BigDecimal balanceBefore = wallet.getBalance();

            // Refund the deposit amount
            wallet.deposit(order.getDepositAmount());
            userWalletRepository.save(wallet);

            // Create refund transaction record
            WalletTransaction refundTransaction = WalletTransaction.builder()
                    .wallet(wallet)
                    .transactionType(TransactionType.ORDER_REFUND)
                    .amount(order.getDepositAmount())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(wallet.getBalance())
                    .order(order)
                    .description(String.format("Refund for cancelled order %s", order.getOrderNumber()))
                    .referenceNumber(order.getOrderNumber())
                    .build();

            walletTransactionRepository.save(refundTransaction);

            log.info("Refunded {} to user {} wallet for cancelled order {}",
                    order.getDepositAmount(), userId, orderId);
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

    /**
     * Pay remaining amount (30% + fees) from wallet
     * @param userId User ID (for security check)
     * @param orderId Order ID
     * @return OrderResponse DTO
     */
    @Transactional
    public OrderResponse payRemainingAmount(Long userId, Long orderId) {
        log.info("User {} paying remaining amount for order {}", userId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to pay for order {} belonging to user {}",
                    userId, orderId, order.getUserId());
            throw new OrderNotFoundException(orderId);
        }

        // Check if remaining payment is needed
        if (order.getPaymentStatus() == OrderPaymentStatus.WALLET_PAID ||
            order.getPaymentStatus() == OrderPaymentStatus.FULLY_COMPLETED) {
            throw new IllegalStateException("Order is already fully paid");
        }

        if (order.getRemainingAmount() == null || order.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No remaining amount to pay");
        }

        // Get user's wallet
        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        // Check if wallet is locked
        if (wallet.isLocked()) {
            throw new IllegalStateException("Wallet is locked. Cannot process payment.");
        }

        // Check wallet balance
        if (wallet.getBalance().compareTo(order.getRemainingAmount()) < 0) {
            throw new RuntimeException(String.format(
                    "Insufficient wallet balance. Required: %s, Available: %s",
                    order.getRemainingAmount(), wallet.getBalance()
            ));
        }

        // Record balance before deduction
        BigDecimal balanceBefore = wallet.getBalance();

        // Deduct remaining amount from wallet
        wallet.withdraw(order.getRemainingAmount());
        userWalletRepository.save(wallet);

        // Create wallet transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(TransactionType.ORDER_PAYMENT)
                .amount(order.getRemainingAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .order(order)
                .description(String.format("Remaining payment for order %s (30%% + fees)", order.getOrderNumber()))
                .referenceNumber(order.getOrderNumber())
                .build();

        walletTransactionRepository.save(transaction);

        // Update order payment status
        order.setPaymentStatus(OrderPaymentStatus.WALLET_PAID);
        orderRepository.save(order);

        log.info("Paid remaining amount {} for order {} - new wallet balance: {}",
                order.getRemainingAmount(), orderId, wallet.getBalance());

        // Reload with items
        Order orderWithItems = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.fromEntity(orderWithItems);
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Get all orders with optional filters (admin operation)
     * @param pageable Pagination information
     * @param status Optional status filter
     * @param userId Optional user ID filter
     * @return Page of OrderResponse
     */
    public Page<OrderResponse> getAllOrders(Pageable pageable, OrderStatus status, Long userId) {
        log.info("Admin getting all orders - status: {}, userId: {}", status, userId);

        Page<Order> orders;

        if (userId != null && status != null) {
            // Filter by both userId and status
            orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else if (userId != null) {
            // Filter by userId only
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (status != null) {
            // Filter by status only
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            // No filter - get all orders
            orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return orders.map(OrderResponse::fromEntity);
    }

    /**
     * Get order by ID (admin operation - no user check)
     * @param orderId Order ID
     * @return OrderResponse DTO
     */
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        log.info("Admin getting order {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.fromEntity(order);
    }

    /**
     * Update order status (admin operation)
     * Records status change in history with admin note
     * @param orderId Order ID
     * @param request UpdateOrderStatusRequest (status + optional note)
     * @return Updated OrderResponse
     */
    public OrderResponse updateOrderStatusAdmin(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Admin updating order {} status to {} with note: {}",
                orderId, request.getStatus(), request.getNote());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Validate status transition (optional - can add business rules here)
        // For now, admin can change to any status

        // Record status change in history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .previousStatus(previousStatus)
                .note(request.getNote() != null ? request.getNote() : "Status changed by admin")
                .changedBy(AuthenticationUtil.getCurrentUserId()) // Admin user ID
                .build();

        order.getStatusHistory().add(history);
        order.setStatus(newStatus);

        // Update payment status based on order status
        if (newStatus == OrderStatus.DELIVERED) {
            order.setPaymentStatus(OrderPaymentStatus.FULLY_COMPLETED);
        } else if (newStatus == OrderStatus.CANCELLED) {
            // Cancelled orders keep their current payment status
        }

        Order updatedOrder = orderRepository.save(order);

        log.info("Admin updated order {} status from {} to {}",
                orderId, previousStatus, newStatus);

        // Reload with items
        Order orderWithItems = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.fromEntity(orderWithItems);
    }
}
