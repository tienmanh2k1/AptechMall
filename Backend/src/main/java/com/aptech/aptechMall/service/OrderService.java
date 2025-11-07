package com.aptech.aptechMall.service;

import com.aptech.aptechMall.Exception.*;
import com.aptech.aptechMall.dto.exchange.ExchangeRateResponse;
import com.aptech.aptechMall.dto.order.CheckoutRequest;
import com.aptech.aptechMall.dto.order.OrderResponse;
import com.aptech.aptechMall.dto.order.UpdateOrderAddressRequest;
import com.aptech.aptechMall.dto.order.UpdateOrderFeesRequest;
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
 * Service quản lý đơn hàng (Order Management)
 *
 * Chức năng chính:
 * - Checkout: Tạo đơn hàng từ giỏ hàng
 * - Thanh toán: Trừ tiền từ ví điện tử (70% cọc + 30% còn lại)
 * - Quản lý đơn hàng: Xem danh sách, chi tiết đơn hàng
 * - Hủy đơn: Hủy và hoàn tiền về ví
 * - Admin: Quản lý tất cả đơn hàng, cập nhật trạng thái, cập nhật phí
 *
 * LUỒNG ĐẶT HÀNG (Checkout):
 * 1. User chọn sản phẩm trong giỏ → POST /api/orders/checkout
 * 2. System tính tổng tiền sản phẩm (convert USD/CNY → VND)
 * 3. Tính phí dịch vụ 1.5% trên tổng giá trị sản phẩm
 * 4. Tính tiền cọc 70% (deposit) và tiền còn lại 30% (remaining)
 * 5. Kiểm tra số dư ví đủ trả tiền cọc không
 * 6. Trừ tiền cọc từ ví → Tạo transaction ORDER_PAYMENT
 * 7. Lưu đơn hàng với status PENDING
 * 8. Xóa các item đã checkout khỏi giỏ hàng
 *
 * HỆ THỐNG THANH TOÁN:
 * - Deposit (70%): Thanh toán ngay khi checkout
 * - Remaining (30% + phí phát sinh): Thanh toán sau khi admin cập nhật phí
 * - Phí bao gồm: Product cost + Service fee (1.5%) + Shipping fees + Additional services
 *
 * TỶ GIÁ NGOẠI TỆ:
 * - AliExpress: USD → VND (ExchangeRateService)
 * - Alibaba 1688: CNY → VND (ExchangeRateService)
 * - Tỷ giá được cache và cập nhật định kỳ
 *
 * QUẢN LÝ PHÍ (Fee Management):
 * - Service Fee: 1.5% trên tổng giá trị sản phẩm (tự động tính)
 * - Domestic Shipping Fee: Phí ship nội địa Trung Quốc (admin nhập bằng CNY)
 * - International Shipping Fee: Phí ship quốc tế Trung → Việt (admin nhập bằng VND)
 * - Additional Services Fee: Phí dịch vụ thêm (đóng gỗ, bọc bong bóng, đếm hàng, v.v.)
 *
 * VÒNG ĐỜI ĐƠN HÀNG (Order Lifecycle):
 * 1. PENDING - Đơn hàng mới tạo, chờ xác nhận
 * 2. CONFIRMED - Admin xác nhận và order từ marketplace
 * 3. PURCHASING - Đang mua hàng từ marketplace
 * 4. IN_TRANSIT - Hàng đang vận chuyển
 * 5. DELIVERED - Đã giao hàng thành công
 * 6. CANCELLED - Đã hủy (có hoàn tiền nếu đã thanh toán)
 *
 * HỦY ĐƠN VÀ HOÀN TIỀN:
 * - User chỉ hủy được đơn ở trạng thái PENDING
 * - Admin có thể hủy đơn ở bất kỳ trạng thái nào
 * - Khi hủy đơn đã thanh toán → tự động hoàn tiền về ví
 * - Tạo transaction ORDER_REFUND để ghi nhận hoàn tiền
 *
 * BẢO MẬT:
 * - userId được lấy từ JWT token (AuthenticationUtil.getCurrentUserId())
 * - User chỉ xem/sửa đơn hàng của chính mình
 * - Admin/Staff có quyền xem/sửa tất cả đơn hàng
 * - Verify ownership trước mọi thao tác (getOrderDetail, cancelOrder, v.v.)
 *
 * TRANSACTION:
 * - @Transactional: Tất cả methods chạy trong transaction
 * - Checkout là atomic: Tạo order + Trừ tiền ví + Xóa giỏ hàng → cùng commit/rollback
 * - Đảm bảo data consistency (không mất tiền, không mất đơn hàng)
 *
 * STATUS HISTORY:
 * - Mỗi lần thay đổi trạng thái → ghi vào OrderStatusHistory
 * - Ghi nhận: Previous status, New status, Admin note, Changed by (admin userId)
 * - Audit trail đầy đủ để tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional // Tất cả methods chạy trong transaction
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final ExchangeRateService exchangeRateService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserWalletRepository userWalletRepository;
    private final FeeCalculationService feeCalculationService;

    /**
     * Suy ra loại tiền tệ từ marketplace
     *
     * Mỗi marketplace sử dụng tiền tệ riêng:
     * - AliExpress: USD (Dollar Mỹ)
     * - Alibaba 1688: CNY (Nhân dân tệ Trung Quốc)
     *
     * Method này dùng để convert giá sản phẩm sang VND khi tính tổng đơn hàng
     *
     * @param marketplace Marketplace enum (ALIEXPRESS hoặc ALIBABA1688)
     * @return Currency code (USD hoặc CNY)
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
     * Tạo mã đơn hàng duy nhất (Order Number)
     *
     * Format: ORD-{timestamp}-{random}
     * - ORD: Prefix cố định
     * - Timestamp: yyyyMMddHHmmss (14 chữ số)
     * - Random: 4 ký tự hex ngẫu nhiên (0-9, A-F)
     *
     * Ví dụ: ORD-20231025143522-A3F9
     * → Order tạo ngày 25/10/2023 lúc 14:35:22, random code A3F9
     *
     * UNIQUENESS CHECK:
     * - Check database để đảm bảo không trùng (rất hiếm xảy ra)
     * - Nếu trùng → generate lại random part
     *
     * @return Mã đơn hàng unique (String)
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
     * Checkout - Tạo đơn hàng từ giỏ hàng
     *
     * Đây là method phức tạp nhất của OrderService, xử lý toàn bộ quy trình đặt hàng
     *
     * LUỒNG XỬ LÝ:
     * 1. Validate input (địa chỉ, số điện thoại, format SĐT Việt Nam)
     * 2. Kiểm tra user và giỏ hàng tồn tại
     * 3. Filter items theo itemIds (nếu user chỉ checkout một phần giỏ hàng)
     * 4. Tạo Order entity với mã đơn hàng unique
     * 5. Copy cart items → order items
     * 6. Tính tổng tiền sản phẩm (convert sang VND qua exchange rate)
     * 7. Tính phí dịch vụ 1.5% (FeeCalculationService)
     * 8. Tính deposit 70% và remaining 30%
     * 9. Kiểm tra số dư ví và wallet lock status
     * 10. Lưu order → Tạo wallet transaction → Trừ tiền ví
     * 11. Xóa items đã checkout khỏi giỏ hàng
     * 12. Trả về OrderResponse
     *
     * TÍNH TIỀN:
     * - Product Cost = Tổng (item price x quantity) convert sang VND
     * - Service Fee = Product Cost x 1.5%
     * - Total Cost = Product Cost + Service Fee
     * - Deposit (70%) = Total Cost x 70% (làm tròn)
     * - Remaining (30%) = Total Cost - Deposit
     *
     * CONVERT TỶ GIÁ:
     * - Lấy marketplace từ cart item → infer currency (USD/CNY)
     * - Gọi ExchangeRateService để lấy rate → VND
     * - Validate exchange rate phải > 0
     * - Convert và làm tròn về số nguyên (HALF_UP)
     *
     * WALLET PAYMENT:
     * - Check wallet locked → throw exception
     * - Check balance đủ trả deposit → throw exception với số tiền thiếu
     * - Save order TRƯỚC (để có order ID)
     * - Tạo WalletTransaction với type ORDER_PAYMENT
     * - Update wallet balance (atomic với transaction)
     * - Nếu bất kỳ bước nào fail → rollback toàn bộ (@Transactional)
     *
     * XÓA GIỎ HÀNG:
     * - Nếu có itemIds → chỉ xóa items đã checkout
     * - Nếu không có itemIds → xóa toàn bộ giỏ hàng (backward compatibility)
     *
     * ERROR HANDLING:
     * - IllegalArgumentException: Invalid input (address, phone)
     * - UserNotFoundException: User không tồn tại
     * - CartNotFoundException: User chưa có giỏ hàng
     * - EmptyCartException: Giỏ hàng rỗng hoặc không có item nào được chọn
     * - IllegalStateException: Wallet locked, insufficient balance, invalid exchange rate
     *
     * BẢO MẬT:
     * - userId được truyền từ controller (đã extract từ JWT token)
     * - Verify user tồn tại trước khi xử lý
     * - User chỉ checkout giỏ hàng của chính mình (cart.userId == userId)
     *
     * @param userId User ID (từ JWT token, đã authenticated)
     * @param request CheckoutRequest chứa: shipping address, phone, note, itemIds (optional)
     * @return OrderResponse với đầy đủ thông tin đơn hàng và items
     * @throws IllegalArgumentException nếu input không hợp lệ
     * @throws UserNotFoundException nếu user không tồn tại
     * @throws CartNotFoundException nếu giỏ hàng không tồn tại
     * @throws EmptyCartException nếu giỏ hàng rỗng
     * @throws IllegalStateException nếu wallet locked hoặc insufficient balance
     */
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        log.info("Processing checkout for user {}", userId);

        // Validate input parameters (defense in depth - controller has @Valid but service layer should also validate)
        if (request == null) {
            throw new IllegalArgumentException("Checkout request cannot be null");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        // Validate Vietnam phone number format (10 digits, starts with 0)
        String cleanPhone = request.getPhone().replaceAll("\\D", "");
        if (cleanPhone.length() != 10 || !cleanPhone.startsWith("0")) {
            throw new IllegalArgumentException("Invalid phone number format. Must be a valid Vietnam phone number (10 digits, starts with 0)");
        }

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

        // Calculate service fee (1.5% of product cost) - using FeeCalculationService
        BigDecimal serviceFee = feeCalculationService.calculateServiceFee(totalVND);
        order.setServiceFee(serviceFee);

        // Total cost = product cost + service fee
        BigDecimal totalCost = totalVND.add(serviceFee);

        // Calculate deposit (70% of total cost including service fee)
        BigDecimal depositAmount = totalCost.multiply(BigDecimal.valueOf(0.70))
                .setScale(0, java.math.RoundingMode.HALF_UP);
        order.setDepositAmount(depositAmount);

        // Calculate remaining (30% of total cost)
        BigDecimal remainingAmount = totalCost.subtract(depositAmount);
        order.setRemainingAmount(remainingAmount);

        // Set totalAmount (for wallet payment, this is same as deposit for now)
        order.setTotalAmount(depositAmount);

        log.info("Order amounts - Product: {} VND, Service Fee: {} VND (1.5%), Total Cost: {} VND, Deposit: {} VND, Remaining: {} VND",
                totalVND, serviceFee, totalCost, depositAmount, remainingAmount);

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
     * Lấy danh sách đơn hàng của user với phân trang
     *
     * User chỉ xem được đơn hàng của chính mình
     * Đơn hàng được sắp xếp theo createdAt giảm dần (mới nhất trước)
     *
     * PAGINATION:
     * - Pageable chứa: page number, page size, sort
     * - Ví dụ: page=0, size=10 → lấy 10 đơn hàng đầu tiên
     * - Return Page object với totalElements, totalPages, content[]
     *
     * RESPONSE FORMAT:
     * - Dùng OrderResponse.toSummary() để tối ưu performance
     * - Không load OrderItems trong summary list (chỉ load khi getOrderDetail)
     *
     * @param userId User ID (từ JWT token)
     * @param pageable Thông tin phân trang (page, size, sort)
     * @return Page<OrderResponse> với danh sách đơn hàng và metadata
     * @throws UserNotFoundException nếu user không tồn tại
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
     * Lấy chi tiết đơn hàng theo ID
     *
     * Load đầy đủ thông tin đơn hàng bao gồm OrderItems
     *
     * BẢO MẬT:
     * - Verify đơn hàng thuộc về user (order.userId == userId)
     * - Nếu không match → throw OrderNotFoundException (giống như không tìm thấy)
     * - Không expose thông tin "đơn hàng tồn tại nhưng không phải của bạn"
     *
     * @param userId User ID (từ JWT token, để verify ownership)
     * @param orderId Order ID cần xem chi tiết
     * @return OrderResponse với đầy đủ items
     * @throws OrderNotFoundException nếu order không tồn tại hoặc không thuộc về user
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
     * Cập nhật trạng thái đơn hàng (User operation)
     *
     * User có quyền thay đổi trạng thái đơn hàng của chính mình (ít dùng)
     * Thông thường user chỉ dùng cancelOrder() để hủy đơn
     *
     * BẢO MẬT:
     * - Verify ownership trước khi cập nhật
     * - User không thể thay đổi đơn hàng của người khác
     *
     * @param userId User ID (từ JWT token)
     * @param orderId Order ID
     * @param newStatus Trạng thái mới
     * @return OrderResponse đã cập nhật
     * @throws OrderNotFoundException nếu order không tồn tại hoặc không thuộc về user
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
     * Hủy đơn hàng và hoàn tiền về ví
     *
     * User chỉ có thể hủy đơn hàng ở trạng thái PENDING (chưa xác nhận)
     *
     * ĐIỀU KIỆN HỦY ĐƠN:
     * - Order phải ở trạng thái PENDING
     * - Order phải thuộc về user (security check)
     * - Method này gọi order.isCancellable() để validate
     *
     * HOÀN TIỀN TỰ ĐỘNG:
     * - Nếu user đã thanh toán deposit → hoàn tiền về ví
     * - Cộng lại depositAmount vào wallet balance
     * - Tạo WalletTransaction với type ORDER_REFUND
     * - Transaction ghi nhận balance_before và balance_after
     *
     * LUỒNG XỬ LÝ:
     * 1. Load order và verify ownership
     * 2. Check order có thể hủy không (isCancellable)
     * 3. Nếu đã thanh toán deposit:
     *    - Load wallet của user
     *    - Cộng tiền vào wallet (wallet.deposit())
     *    - Save wallet
     *    - Tạo refund transaction record
     * 4. Set order status = CANCELLED
     * 5. Save order và return response
     *
     * TRANSACTION:
     * - Toàn bộ quá trình trong 1 transaction (@Transactional)
     * - Nếu bất kỳ bước nào fail → rollback (không mất tiền, order không bị cancel)
     *
     * BẢO MẬT:
     * - Verify ownership trước khi hủy
     * - User chỉ hủy được đơn hàng của chính mình
     * - Log warning nếu có attempt truy cập order của người khác
     *
     * @param userId User ID (từ JWT token)
     * @param orderId Order ID cần hủy
     * @return OrderResponse với status CANCELLED
     * @throws OrderNotFoundException nếu order không tồn tại hoặc không thuộc về user
     * @throws OrderNotCancellableException nếu order không thể hủy (status không phải PENDING)
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
     * Lấy đơn hàng theo mã đơn hàng (Order Number)
     *
     * Tương tự getOrderDetail nhưng tìm theo orderNumber thay vì orderId
     * Hữu ích khi user muốn tra cứu đơn hàng bằng mã (ví dụ: ORD-20231025143522-A3F9)
     *
     * @param userId User ID (từ JWT token, để verify ownership)
     * @param orderNumber Mã đơn hàng (ORD-xxxxx-xxxx)
     * @return OrderResponse với đầy đủ thông tin
     * @throws OrderNotFoundException nếu order không tồn tại hoặc không thuộc về user
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
     * Thanh toán số tiền còn lại (30% + phí phát sinh) từ ví điện tử
     *
     * Được gọi sau khi admin cập nhật phí ship và phí dịch vụ thêm
     * User cần thanh toán phần còn lại để hoàn tất đơn hàng
     *
     * KỊCH BẢN SỬ DỤNG:
     * 1. User checkout → trả 70% deposit
     * 2. Admin order hàng từ marketplace
     * 3. Admin cập nhật phí ship thực tế và phí dịch vụ thêm (updateOrderFees)
     * 4. System tính lại remainingAmount = total - deposit
     * 5. User gọi API này để trả phần còn lại
     * 6. Order chuyển sang WALLET_PAID hoặc FULLY_COMPLETED
     *
     * REMAINING AMOUNT BAO GỒM:
     * - 30% giá trị sản phẩm ban đầu
     * - Phí ship nội địa (domestic shipping fee)
     * - Phí ship quốc tế (international shipping fee)
     * - Phí dịch vụ thêm (additional services: đóng gỗ, bọc bong bóng, v.v.)
     *
     * ĐIỀU KIỆN THANH TOÁN:
     * - Order chưa thanh toán hết (paymentStatus != WALLET_PAID/FULLY_COMPLETED)
     * - remainingAmount > 0
     * - Wallet không bị lock
     * - Wallet balance đủ trả remainingAmount
     *
     * LUỒNG XỬ LÝ:
     * 1. Load order và verify ownership
     * 2. Check payment status (chưa thanh toán hết)
     * 3. Check remainingAmount > 0
     * 4. Load wallet và check locked/balance
     * 5. Trừ tiền từ wallet (wallet.withdraw)
     * 6. Tạo WalletTransaction với type ORDER_PAYMENT
     * 7. Update order.paymentStatus = WALLET_PAID
     * 8. Save và return OrderResponse
     *
     * TRANSACTION:
     * - @Transactional: Atomic operation
     * - Nếu fail → rollback (không mất tiền, payment status không đổi)
     *
     * @param userId User ID (từ JWT token)
     * @param orderId Order ID cần thanh toán
     * @return OrderResponse với payment status đã cập nhật
     * @throws OrderNotFoundException nếu order không tồn tại hoặc không thuộc về user
     * @throws IllegalStateException nếu đã thanh toán hết, không có remaining amount, wallet locked, hoặc insufficient balance
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
     * Lấy tất cả đơn hàng với filter tùy chọn (Admin operation)
     *
     * Admin/Staff có thể xem tất cả đơn hàng của tất cả user
     * Hỗ trợ filter theo status và userId
     *
     * FILTER OPTIONS:
     * - Không filter: Lấy tất cả đơn hàng
     * - Filter theo status: Chỉ lấy đơn ở trạng thái cụ thể (PENDING, CONFIRMED, v.v.)
     * - Filter theo userId: Chỉ lấy đơn của 1 user cụ thể
     * - Combine cả 2: userId + status
     *
     * PAGINATION:
     * - Pageable chứa page, size, sort
     * - Đơn hàng sắp xếp theo createdAt giảm dần (mới nhất trước)
     *
     * USE CASE:
     * - Admin dashboard: Xem tất cả đơn hàng pending để xử lý
     * - Search orders: Tìm đơn hàng của user cụ thể
     * - Reports: Thống kê đơn hàng theo status
     *
     * @param pageable Thông tin phân trang
     * @param status (Optional) Filter theo order status
     * @param userId (Optional) Filter theo user ID
     * @return Page<OrderResponse> với danh sách đơn hàng
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
     * Lấy đơn hàng theo ID (Admin operation - không check ownership)
     *
     * Khác với getOrderDetail (user), method này KHÔNG kiểm tra ownership
     * Admin có thể xem đơn hàng của bất kỳ user nào
     *
     * @param orderId Order ID
     * @return OrderResponse với đầy đủ thông tin
     * @throws OrderNotFoundException nếu order không tồn tại
     */
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        log.info("Admin getting order {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.fromEntity(order);
    }

    /**
     * Cập nhật trạng thái đơn hàng (Admin operation)
     *
     * Admin có quyền thay đổi trạng thái đơn hàng của bất kỳ user nào
     * Mỗi lần thay đổi trạng thái được ghi lại trong OrderStatusHistory
     *
     * STATUS HISTORY TRACKING:
     * - Ghi nhận previous status và new status
     * - Ghi nhận admin note (lý do thay đổi)
     * - Ghi nhận changedBy (admin user ID từ JWT token)
     * - Timestamp tự động (createdAt)
     *
     * AUTO PAYMENT STATUS UPDATE:
     * - Nếu order → DELIVERED: set paymentStatus = FULLY_COMPLETED
     * - Nếu order → CANCELLED: giữ nguyên payment status (refund xử lý riêng)
     *
     * VÒNG ĐỜI THÔNG THƯỜNG:
     * 1. PENDING → CONFIRMED (admin xác nhận order)
     * 2. CONFIRMED → PURCHASING (đang order từ marketplace)
     * 3. PURCHASING → IN_TRANSIT (hàng đang ship)
     * 4. IN_TRANSIT → DELIVERED (giao hàng thành công)
     * 5. Có thể CANCELLED ở bất kỳ bước nào
     *
     * BUSINESS RULES:
     * - Admin có thể chuyển sang bất kỳ status nào (không validate transition)
     * - Có thể thêm business rules nếu cần (ví dụ: không cho phép DELIVERED → PENDING)
     *
     * @param orderId Order ID cần cập nhật
     * @param request UpdateOrderStatusRequest chứa: status (bắt buộc), note (optional)
     * @return OrderResponse đã cập nhật với status history
     * @throws OrderNotFoundException nếu order không tồn tại
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

    /**
     * Cập nhật phí đơn hàng (Admin/Staff operation)
     *
     * Admin/Staff cập nhật phí ship thực tế và phí dịch vụ thêm sau khi order từ marketplace
     * Method này tính toán lại tổng chi phí và remaining amount
     *
     * ĐIỀU KIỆN CẬP NHẬT PHÍ:
     * - Order phải ở trạng thái CONFIRMED
     * - Chỉ admin/staff mới có quyền cập nhật
     *
     * CÁC LOẠI PHÍ CÓ THỂ CẬP NHẬT:
     * 1. Domestic Shipping Fee (phí ship nội địa):
     *    - Admin nhập bằng CNY
     *    - System tự động convert CNY → VND qua ExchangeRateService
     *
     * 2. International Shipping Fee (phí ship quốc tế):
     *    - Admin nhập bằng VND (không cần convert)
     *
     * 3. Estimated Weight (cân nặng ước tính):
     *    - Dùng để tính phí dịch vụ thêm
     *
     * 4. Additional Services:
     *    - Wooden Packaging (đóng gỗ): ✓/✗
     *    - Bubble Wrap (bọc bong bóng): ✓/✗
     *    - Item Count Check (đếm hàng): ✓/✗
     *    - FeeCalculationService tự động tính phí dựa trên services được chọn
     *
     * TÍNH LẠI TỔNG CHI PHÍ:
     * - Product Cost (giá sản phẩm - đã tính khi checkout)
     * - Service Fee (1.5% - đã tính khi checkout)
     * - Domestic Shipping Fee (phí ship nội địa - mới cập nhật)
     * - International Shipping Fee (phí ship quốc tế - mới cập nhật)
     * - Additional Services Fee (phí dịch vụ thêm - tính dựa trên options)
     * → Total Amount = tổng tất cả phí trên
     * → Remaining Amount = Total - Deposit (đã trả 70%)
     *
     * LUỒNG XỬ LÝ:
     * 1. Load order và check status = CONFIRMED
     * 2. Cập nhật domestic shipping fee (convert CNY → VND)
     * 3. Cập nhật international shipping fee
     * 4. Cập nhật estimated weight
     * 5. Tính additional services fee (FeeCalculationService)
     * 6. Tính lại total amount
     * 7. Tính lại remaining amount (total - deposit)
     * 8. Thêm admin note vào order note (nếu có)
     * 9. Save order và return response
     *
     * SAU KHI CẬP NHẬT PHÍ:
     * - User có thể xem remaining amount mới
     * - User gọi payRemainingAmount() để thanh toán phần còn lại
     *
     * @param orderId Order ID cần cập nhật phí
     * @param request UpdateOrderFeesRequest chứa: domestic fee, international fee, weight, services, note
     * @return OrderResponse với phí đã cập nhật
     * @throws OrderNotFoundException nếu order không tồn tại
     * @throws IllegalStateException nếu order không ở trạng thái CONFIRMED
     */
    public OrderResponse updateOrderFees(Long orderId, UpdateOrderFeesRequest request) {
        log.info("Admin/staff updating order {} fees", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Only allow fee updates when order is CONFIRMED
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Chỉ có thể cập nhật phí khi đơn hàng ở trạng thái CONFIRMED. " +
                    "Trạng thái hiện tại: " + order.getStatus()
            );
        }

        // Update domestic shipping fee (from China warehouse to port)
        if (request.getDomesticShippingFee() != null) {
            // Convert CNY to VND
            BigDecimal domesticFeeVND = exchangeRateService.convertCurrency(
                    request.getDomesticShippingFee(),
                    "CNY",
                    "VND"
            );
            order.setDomesticShippingFee(domesticFeeVND);
            log.info("Updated domestic shipping fee: {} CNY = {} VND",
                    request.getDomesticShippingFee(), domesticFeeVND);
        }

        // Update international shipping fee (China to Vietnam)
        if (request.getInternationalShippingFee() != null) {
            order.setInternationalShippingFee(request.getInternationalShippingFee());
            log.info("Updated international shipping fee: {} VND", request.getInternationalShippingFee());
        }

        // Update estimated weight
        if (request.getEstimatedWeight() != null) {
            order.setEstimatedWeight(request.getEstimatedWeight());
            log.info("Updated estimated weight: {} kg", request.getEstimatedWeight());
        }

        // Calculate additional services fees
        BigDecimal additionalServicesFee = feeCalculationService.calculateTotalAdditionalServicesFee(
                order.getItems(),
                request.getEstimatedWeight() != null ? request.getEstimatedWeight() : BigDecimal.ZERO,
                request.getIncludeWoodenPackaging() != null && request.getIncludeWoodenPackaging(),
                request.getIncludeBubbleWrap() != null && request.getIncludeBubbleWrap(),
                request.getIncludeItemCountCheck() != null && request.getIncludeItemCountCheck()
        );

        // Store additional services fee in order
        order.setAdditionalServicesFee(additionalServicesFee);
        log.info("Additional services fee: {} VND", additionalServicesFee);

        // Update note if provided
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            String currentNote = order.getNote() != null ? order.getNote() : "";
            order.setNote(currentNote + "\n[Admin/Staff Note] " + request.getNote());
        }

        // Recalculate total amount
        BigDecimal productCost = order.getProductCost() != null ? order.getProductCost() : BigDecimal.ZERO;
        BigDecimal serviceFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
        BigDecimal domesticShipping = order.getDomesticShippingFee() != null ? order.getDomesticShippingFee() : BigDecimal.ZERO;
        BigDecimal internationalShipping = order.getInternationalShippingFee() != null ? order.getInternationalShippingFee() : BigDecimal.ZERO;

        BigDecimal totalAmount = productCost
                .add(serviceFee)
                .add(domesticShipping)
                .add(internationalShipping)
                .add(additionalServicesFee);

        order.setTotalAmount(totalAmount);

        // Recalculate remaining amount (total - deposit already paid)
        BigDecimal depositAmount = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = totalAmount.subtract(depositAmount);
        order.setRemainingAmount(remainingAmount);

        log.info("Updated order total: Product={}, Service={}, Domestic={}, International={}, Additional={}, Total={}, Deposit={}, Remaining={}",
                productCost, serviceFee, domesticShipping, internationalShipping, additionalServicesFee,
                totalAmount, depositAmount, remainingAmount);

        Order updatedOrder = orderRepository.save(order);

        return OrderResponse.fromEntity(updatedOrder);
    }

    /**
     * Cập nhật địa chỉ giao hàng (User operation)
     *
     * User có thể thay đổi địa chỉ và số điện thoại khi đơn hàng chưa được xác nhận
     *
     * ĐIỀU KIỆN THAY ĐỔI:
     * - Order phải thuộc về user (ownership check)
     * - Order phải ở trạng thái PENDING (chưa xác nhận)
     * - Nếu đã CONFIRMED hoặc muộn hơn → không cho phép thay đổi
     *
     * USE CASE:
     * - User vừa checkout xong, phát hiện sai địa chỉ
     * - User muốn giao hàng đến địa chỉ khác
     * - Admin chưa xác nhận đơn → user còn có thể sửa
     *
     * LUỒNG XỬ LÝ:
     * 1. Load order và verify ownership
     * 2. Check status = PENDING (nếu không → throw exception)
     * 3. Update shippingAddress và phone
     * 4. Thêm note ghi nhận thay đổi (nếu user cung cấp note)
     * 5. Save và return OrderResponse
     *
     * BẢO MẬT:
     * - Verify ownership (order.userId == userId)
     * - User chỉ sửa được đơn hàng của chính mình
     *
     * @param userId User ID (từ JWT token)
     * @param orderId Order ID cần sửa
     * @param request UpdateOrderAddressRequest chứa: shippingAddress, phone, note (optional)
     * @return OrderResponse đã cập nhật
     * @throws IllegalArgumentException nếu không phải đơn hàng của user
     * @throws IllegalStateException nếu order không ở trạng thái PENDING
     */
    public OrderResponse updateOrderAddress(Long userId, Long orderId, UpdateOrderAddressRequest request) {
        log.info("User {} updating order {} address", userId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền thay đổi địa chỉ đơn hàng này");
        }

        // Only allow address updates when order is PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể thay đổi địa chỉ khi đơn hàng ở trạng thái PENDING. " +
                    "Trạng thái hiện tại: " + order.getStatus()
            );
        }

        // Update address and phone
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());

        // Add note if provided
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            String currentNote = order.getNote() != null ? order.getNote() : "";
            order.setNote(currentNote + "\n[Đã thay đổi địa chỉ] " + request.getNote());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("User {} updated order {} address successfully", userId, orderId);

        return OrderResponse.fromEntity(updatedOrder);
    }

    /**
     * Cập nhật địa chỉ giao hàng (Admin/Staff operation)
     *
     * Admin/Staff có thể thay đổi địa chỉ giao hàng khi đơn hàng ở PENDING hoặc CONFIRMED
     * Linh hoạt hơn user operation (user chỉ sửa được khi PENDING)
     *
     * ĐIỀU KIỆN THAY ĐỔI:
     * - Order ở trạng thái PENDING hoặc CONFIRMED
     * - Không check ownership (admin có thể sửa đơn hàng bất kỳ)
     *
     * USE CASE:
     * - User liên hệ admin để đổi địa chỉ sau khi admin đã xác nhận đơn
     * - Admin phát hiện sai sót trong địa chỉ cần sửa lại
     * - Địa chỉ không hợp lệ, admin cần xác nhận lại với user
     *
     * PHÂN BIỆT VỚI USER OPERATION:
     * - User: Chỉ sửa được khi PENDING
     * - Admin: Sửa được khi PENDING hoặc CONFIRMED
     * - Note có prefix "[Admin/Staff đã thay đổi địa chỉ]" để phân biệt
     *
     * @param orderId Order ID cần sửa
     * @param request UpdateOrderAddressRequest chứa: shippingAddress, phone, note (optional)
     * @return OrderResponse đã cập nhật
     * @throws OrderNotFoundException nếu order không tồn tại
     * @throws IllegalStateException nếu order không ở trạng thái PENDING hoặc CONFIRMED
     */
    public OrderResponse updateOrderAddressAdmin(Long orderId, UpdateOrderAddressRequest request) {
        log.info("Admin/staff updating order {} address", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Admin/Staff can update address when PENDING or CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Chỉ có thể thay đổi địa chỉ khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED. " +
                    "Trạng thái hiện tại: " + order.getStatus()
            );
        }

        // Update address and phone
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());

        // Add note if provided
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            String currentNote = order.getNote() != null ? order.getNote() : "";
            order.setNote(currentNote + "\n[Admin/Staff đã thay đổi địa chỉ] " + request.getNote());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Admin/staff updated order {} address successfully", orderId);

        return OrderResponse.fromEntity(updatedOrder);
    }
}
