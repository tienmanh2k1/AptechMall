package com.aptech.aptechMall.service.wallet;

import com.aptech.aptechMall.dto.wallet.*;
import com.aptech.aptechMall.entity.*;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.repository.UserWalletRepository;
import com.aptech.aptechMall.repository.WalletTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý ví điện tử (E-Wallet) và giao dịch
 *
 * Chức năng chính:
 * - Tạo và quản lý ví cho user
 * - Nạp tiền vào ví (Deposit)
 * - Xem lịch sử giao dịch với filter
 * - Admin: Khóa/mở khóa ví, xem tất cả ví
 *
 * VÍ ĐIỆN TỬ (E-Wallet):
 * - Mỗi user có 1 wallet duy nhất (One-to-One relationship)
 * - Wallet tự động tạo khi user cần dùng (lazy initialization)
 * - Wallet có balance (số dư), isLocked (trạng thái khóa)
 *
 * GIAO DỊCH (Transactions):
 * - DEPOSIT: Nạp tiền vào ví (qua payment gateway hoặc bank transfer)
 * - WITHDRAWAL: Rút tiền khỏi ví (chưa implement)
 * - ORDER_PAYMENT: Trả tiền đơn hàng
 * - ORDER_REFUND: Hoàn tiền khi hủy đơn hàng
 * - ADMIN_ADJUSTMENT: Admin điều chỉnh số dư thủ công
 *
 * LUỒNG NẠP TIỀN (Deposit Flow):
 * 1. User gọi POST /api/wallet/deposit/initiate với amount và paymentGateway
 * 2. WalletService tạo transactionCode và paymentUrl
 * 3. User được redirect đến payment gateway (VNPay/MoMo/ZaloPay/BankTransfer)
 * 4. User thanh toán tại gateway
 * 5. Gateway gọi callback endpoint: POST /api/wallet/deposit/callback
 * 6. WalletService.processDeposit() cộng tiền vào ví và tạo transaction record
 *
 * PAYMENT GATEWAYS HỖ TRỢ:
 * - VNPay (sandbox): https://sandbox.vnpayment.vn
 * - MoMo (test): https://test-payment.momo.vn
 * - ZaloPay (sandbox): https://sandbox.zalopay.vn
 * - Bank Transfer (SMS integration): Nạp tiền qua chuyển khoản + SMS forwarding
 *
 * DEPOSIT CODE:
 * - Mã định danh user để nạp tiền qua bank transfer
 * - Format: USERNAME (uppercase, alphanumeric) hoặc USER{userId}
 * - Ví dụ: VanA → VANA, demo.account@gmail.com → USER123
 * - Dùng trong SMS banking: "Nap tien VANA" hoặc "Nap tien USER123"
 *
 * RACE CONDITION HANDLING:
 * - getOrCreateWallet() xử lý race condition khi nhiều request cùng tạo wallet
 * - Dùng DataIntegrityViolationException catch để retry find wallet
 * - Đảm bảo mỗi user chỉ có đúng 1 wallet (unique constraint: wallet.user_id)
 *
 * WALLET LOCK (Khóa Ví):
 * - Admin có thể khóa ví để prevent fraudulent activity
 * - Khi ví bị khóa: Không deposit, không withdrawal, không order payment
 * - User vẫn xem được balance và transaction history
 * - Admin unlock để mở lại
 *
 * TRANSACTION HISTORY:
 * - Mỗi transaction ghi nhận: amount, balance_before, balance_after
 * - Audit trail đầy đủ: Ai thực hiện, lúc nào, lý do gì
 * - Hỗ trợ filter: Theo transaction type, theo date range
 * - Phân trang: Pageable (page, size)
 *
 * BẢO MẬT:
 * - userId được extract từ JWT token (không accept từ client)
 * - User chỉ xem được wallet và transactions của chính mình
 * - Admin có quyền xem tất cả wallets và lock/unlock
 * - Verify ownership trước khi truy cập transaction
 *
 * TRANSACTION (@Transactional):
 * - Tất cả deposit/withdrawal operations trong transaction
 * - Atomic: Update wallet balance + Create transaction record → cùng commit/rollback
 * - Đảm bảo không mất tiền, không duplicate transaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Lấy hoặc tạo ví cho user (Thread-safe)
     *
     * Method này xử lý race condition khi nhiều request cùng lúc cố gắng tạo wallet
     *
     * RACE CONDITION SCENARIO:
     * - Request A: Check wallet không tồn tại → chuẩn bị tạo wallet
     * - Request B: Check wallet không tồn tại → chuẩn bị tạo wallet
     * - Request A: Insert wallet vào database → thành công
     * - Request B: Insert wallet vào database → FAIL (duplicate key: user_id)
     * - Request B: Catch DataIntegrityViolationException → retry find wallet → thành công
     *
     * LUỒNG XỬ LÝ:
     * 1. Try find existing wallet
     * 2. Nếu tìm thấy → return wallet
     * 3. Nếu không tìm thấy:
     *    a. Load user entity
     *    b. Tạo wallet mới với balance = 0, isLocked = false
     *    c. Save wallet vào database
     *    d. Nếu success → return saved wallet
     *    e. Nếu fail với DataIntegrityViolationException (duplicate):
     *       - Log warning về race condition
     *       - Retry find wallet (wallet đã được tạo bởi thread khác)
     *       - Return wallet tìm được
     *
     * UNIQUE CONSTRAINT:
     * - Database có unique constraint trên wallet.user_id
     * - Đảm bảo mỗi user chỉ có đúng 1 wallet
     * - Nếu 2 threads cùng cố gắng tạo → 1 thành công, 1 fail và retry
     *
     * @param userId User ID
     * @return UserWallet entity (existing hoặc newly created)
     * @throws RuntimeException nếu user không tồn tại hoặc wallet creation failed và retry không tìm thấy
     */
    @Transactional
    public UserWallet getOrCreateWallet(Long userId) {
        // First, try to find existing wallet
        return walletRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    try {
                        // Wallet doesn't exist, create new one
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                        UserWallet wallet = UserWallet.builder()
                                .user(user)
                                .balance(BigDecimal.ZERO)
                                .isLocked(false)
                                .build();

                        UserWallet savedWallet = walletRepository.save(wallet);
                        log.info("Created new wallet for user {}: walletId={}", userId, savedWallet.getId());
                        return savedWallet;

                    } catch (DataIntegrityViolationException e) {
                        // Another thread created the wallet while we were trying
                        // This is a race condition - retry finding the wallet
                        log.warn("Race condition detected while creating wallet for user {}. Retrying find...", userId);
                        return walletRepository.findByUserUserId(userId)
                                .orElseThrow(() -> new RuntimeException(
                                    "Wallet creation failed and wallet still not found for user: " + userId));
                    }
                });
    }

    /**
     * Lấy thông tin ví điện tử của user
     *
     * Return WalletResponse với balance, lock status, và deposit code
     * Nếu user chưa có wallet → tự động tạo mới
     *
     * @param userId User ID
     * @return WalletResponse chứa walletId, userId, balance, isLocked, depositCode, timestamps
     */
    public WalletResponse getWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);

        // Generate deposit code (username or USER{id})
        String depositCode = generateDepositCode(wallet.getUser());

        return WalletResponse.builder()
                .walletId(wallet.getId())
                .userId(userId)
                .balance(wallet.getBalance())
                .isLocked(wallet.isLocked())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .depositCode(depositCode)
                .build();
    }

    /**
     * Tạo mã nạp tiền (Deposit Code) cho bank transfer
     *
     * Deposit code dùng để định danh user khi nạp tiền qua chuyển khoản ngân hàng
     * User ghi mã này vào nội dung chuyển khoản: "Nap tien {depositCode}"
     *
     * FORMAT:
     * - Ưu tiên: USERNAME (uppercase, chỉ giữ alphanumeric)
     * - Fallback: USER{userId} (nếu username null/empty hoặc toàn ký tự đặc biệt)
     *
     * VÍ DỤ:
     * - username = "VanA" → "VANA"
     * - username = "demo.account" → "DEMOACCOUNT"
     * - username = "@@@" → "USER123" (fallback, userId=123)
     * - username = null → "USER456" (fallback, userId=456)
     *
     * ALPHANUMERIC ONLY:
     * - Remove tất cả ký tự đặc biệt (@, ., -, _, v.v.)
     * - Chỉ giữ A-Z, 0-9
     * - Safe để dùng trong SMS banking content
     *
     * @param user User entity
     * @return Deposit code (uppercase alphanumeric hoặc USER{id})
     */
    private String generateDepositCode(User user) {
        // If user has username, use it (uppercase, alphanumeric only)
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            String username = user.getUsername()
                    .toUpperCase()
                    .replaceAll("[^A-Z0-9]", ""); // Remove non-alphanumeric chars
            if (!username.isEmpty()) {
                return username;
            }
        }

        // Fallback: USER{userId}
        return "USER" + user.getUserId();
    }

    /**
     * Khởi tạo giao dịch nạp tiền (Deposit Initiation)
     *
     * Bước đầu tiên của deposit flow - tạo payment URL để user thanh toán
     *
     * LUỒNG XỬ LÝ:
     * 1. Lấy wallet của user (tạo mới nếu chưa có)
     * 2. Check wallet có bị lock không (nếu lock → throw exception)
     * 3. Generate unique transactionCode (16 ký tự hex uppercase)
     * 4. Build paymentUrl dựa vào payment gateway được chọn
     * 5. Return DepositInitiateResponse với paymentUrl
     *
     * TRANSACTION CODE:
     * - UUID random, remove dashes, lấy 16 ký tự đầu, uppercase
     * - Ví dụ: "A3F9B2C1D4E5F6A7"
     * - Dùng để track giao dịch khi gateway callback
     *
     * PAYMENT URL:
     * - VNPay: redirect đến VNPay payment page
     * - MoMo: redirect đến MoMo payment page
     * - ZaloPay: redirect đến ZaloPay payment page
     * - BankTransfer: hiển thị hướng dẫn chuyển khoản
     *
     * SAU KHI INITIATE:
     * - Frontend redirect user đến paymentUrl
     * - User thanh toán tại payment gateway
     * - Gateway gọi callback endpoint với transaction result
     * - processDeposit() xử lý callback và cộng tiền vào ví
     *
     * @param userId User ID (từ JWT token)
     * @param request DepositRequest chứa: amount, paymentGateway, returnUrl
     * @return DepositInitiateResponse với paymentUrl, transactionCode, amount, gateway
     * @throws IllegalStateException nếu wallet bị lock
     */
    @Transactional
    public DepositInitiateResponse initiateDeposit(Long userId, DepositRequest request) {
        UserWallet wallet = getOrCreateWallet(userId);

        if (wallet.isLocked()) {
            throw new IllegalStateException("Wallet is locked. Cannot initiate deposit.");
        }

        // Generate unique transaction code
        String transactionCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // Build payment URL based on gateway
        String paymentUrl = buildPaymentUrl(request.getPaymentGateway(), transactionCode, request.getAmount(), request.getReturnUrl());

        log.info("Initiated deposit for user {}: amount={}, gateway={}, code={}",
                userId, request.getAmount(), request.getPaymentGateway(), transactionCode);

        return DepositInitiateResponse.builder()
                .amount(request.getAmount())
                .paymentGateway(request.getPaymentGateway())
                .paymentUrl(paymentUrl)
                .transactionCode(transactionCode)
                .message("Please complete payment at the payment gateway")
                .build();
    }

    /**
     * Xử lý nạp tiền sau khi thanh toán thành công (Process Deposit Callback)
     *
     * Method này được gọi bởi payment gateway callback hoặc SMS banking webhook
     * Cộng tiền vào ví và tạo transaction record
     *
     * LUỒNG XỬ LÝ:
     * 1. Lấy wallet của user
     * 2. Check wallet có bị lock không (nếu lock → throw exception)
     * 3. Ghi nhận balance_before (số dư trước khi nạp)
     * 4. Cộng tiền vào wallet (wallet.deposit(amount))
     * 5. Save wallet với balance mới
     * 6. Tạo WalletTransaction record với type DEPOSIT
     * 7. Save transaction và return response
     *
     * WALLET TRANSACTION RECORD:
     * - transactionType: DEPOSIT
     * - amount: Số tiền nạp
     * - balanceBefore: Số dư trước khi nạp
     * - balanceAfter: Số dư sau khi nạp (= balanceBefore + amount)
     * - description: "Deposit via {gateway}"
     * - referenceNumber: Mã giao dịch từ gateway (transaction code, GD number, v.v.)
     *
     * REFERENCE NUMBER:
     * - VNPay/MoMo/ZaloPay: Transaction ID từ gateway
     * - Bank Transfer: GD number từ SMS (ví dụ: "GD:123456")
     * - Dùng để reconciliation và tracking
     *
     * TRANSACTION (@Transactional):
     * - Atomic operation: Update wallet balance + Create transaction record
     * - Nếu fail → rollback (không mất tiền, không có transaction record)
     * - Đảm bảo consistency: balance luôn khớp với sum of transactions
     *
     * CALLER:
     * - WalletController.depositCallback() - khi gateway callback
     * - BankTransferController.smsWebhook() - khi nhận SMS banking
     *
     * @param userId User ID
     * @param amount Số tiền nạp (VND)
     * @param paymentGateway Payment gateway đã sử dụng (VNPay, MoMo, ZaloPay, BankTransfer)
     * @param referenceNumber Reference number từ gateway để tracking
     * @return WalletTransactionResponse với transaction details
     * @throws IllegalStateException nếu wallet bị lock
     */
    @Transactional
    public WalletTransactionResponse processDeposit(Long userId, BigDecimal amount,
                                                     PaymentGateway paymentGateway, String referenceNumber) {
        UserWallet wallet = getOrCreateWallet(userId);

        if (wallet.isLocked()) {
            throw new IllegalStateException("Wallet is locked. Cannot process deposit.");
        }

        // Record balance before deposit
        BigDecimal balanceBefore = wallet.getBalance();

        // Perform deposit
        wallet.deposit(amount);
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .description(String.format("Deposit via %s", paymentGateway.name()))
                .referenceNumber(referenceNumber)
                .build();

        WalletTransaction savedTransaction = transactionRepository.save(transaction);

        log.info("Processed deposit for user {}: amount={}, gateway={}, ref={}, newBalance={}",
                userId, amount, paymentGateway, referenceNumber, wallet.getBalance());

        return mapToTransactionResponse(savedTransaction);
    }

    /**
     * Lấy lịch sử giao dịch ví với filter
     *
     * User xem lịch sử giao dịch của ví điện tử với các filter tùy chọn
     *
     * FILTER OPTIONS:
     * - Không filter: Lấy tất cả transactions, sắp xếp theo createdAt giảm dần
     * - Filter theo transaction type: Chỉ lấy DEPOSIT, ORDER_PAYMENT, ORDER_REFUND, v.v.
     * - Filter theo date range: Lấy transactions trong khoảng startDate → endDate
     * - Pagination: Page number và page size
     *
     * TRANSACTION TYPES:
     * - DEPOSIT: Nạp tiền vào ví
     * - WITHDRAWAL: Rút tiền khỏi ví
     * - ORDER_PAYMENT: Trả tiền đơn hàng
     * - ORDER_REFUND: Hoàn tiền khi hủy đơn hàng
     * - ADMIN_ADJUSTMENT: Admin điều chỉnh số dư
     *
     * RESPONSE FORMAT:
     * - isCredit: true nếu transaction cộng tiền vào ví (DEPOSIT, ORDER_REFUND)
     * - isDebit: true nếu transaction trừ tiền khỏi ví (WITHDRAWAL, ORDER_PAYMENT)
     * - balanceBefore, balanceAfter: Audit trail để verify
     *
     * @param userId User ID (từ JWT token)
     * @param filter TransactionFilterRequest chứa: page, size, transactionType, startDate, endDate
     * @return Page<WalletTransactionResponse> với danh sách transactions và metadata
     */
    public Page<WalletTransactionResponse> getTransactionHistory(Long userId, TransactionFilterRequest filter) {
        UserWallet wallet = getOrCreateWallet(userId);

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());
        Page<WalletTransaction> transactions;

        // Apply filters
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            // Filter by date range
            transactions = transactionRepository.findByWalletIdAndDateRange(
                    wallet.getId(),
                    filter.getStartDate(),
                    filter.getEndDate(),
                    pageable);
        } else if (filter.getTransactionType() != null) {
            // Filter by transaction type
            transactions = transactionRepository.findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
                    wallet.getId(),
                    filter.getTransactionType(),
                    pageable);
        } else {
            // No filter - get all transactions
            transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc(
                    wallet.getId(),
                    pageable);
        }

        return transactions.map(this::mapToTransactionResponse);
    }

    /**
     * Lấy chi tiết 1 transaction theo ID
     *
     * BẢO MẬT:
     * - Verify transaction thuộc về wallet của user
     * - User chỉ xem được transaction của chính mình
     * - Nếu không match → throw exception
     *
     * @param userId User ID (từ JWT token, để verify ownership)
     * @param transactionId Transaction ID cần xem chi tiết
     * @return WalletTransactionResponse với đầy đủ thông tin
     * @throws RuntimeException nếu transaction không tồn tại
     * @throws IllegalStateException nếu transaction không thuộc về user
     */
    public WalletTransactionResponse getTransaction(Long userId, Long transactionId) {
        WalletTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));

        // Verify transaction belongs to user
        if (!transaction.getWallet().getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Transaction does not belong to user");
        }

        return mapToTransactionResponse(transaction);
    }

    /**
     * Khóa ví điện tử (Admin operation)
     *
     * Admin khóa ví để prevent fraudulent activity hoặc khi phát hiện vi phạm
     *
     * KHI VÍ BỊ KHÓA:
     * - User KHÔNG thể deposit (nạp tiền)
     * - User KHÔNG thể order payment (trả tiền đơn hàng)
     * - User vẫn xem được balance và transaction history
     * - Admin có thể unlock để mở lại
     *
     * USE CASE:
     * - Phát hiện giao dịch đáng ngờ
     * - User vi phạm chính sách
     * - Request từ user để freeze tài khoản tạm thời
     *
     * @param userId User ID cần khóa ví
     */
    @Transactional
    public void lockWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.lock();
        walletRepository.save(wallet);
        log.info("Locked wallet for user {}", userId);
    }

    /**
     * Mở khóa ví điện tử (Admin operation)
     *
     * Admin mở lại ví đã bị khóa
     * Sau khi unlock, user có thể deposit và order payment bình thường
     *
     * @param userId User ID cần mở khóa ví
     */
    @Transactional
    public void unlockWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.unlock();
        walletRepository.save(wallet);
        log.info("Unlocked wallet for user {}", userId);
    }

    /**
     * Lấy tất cả ví điện tử (Admin operation)
     *
     * Admin xem danh sách tất cả ví của tất cả user
     * Bao gồm thông tin user (username, email, fullName) để dễ quản lý
     *
     * RESPONSE INCLUDES:
     * - walletId, userId
     * - username, email, fullName (user info)
     * - balance (số dư hiện tại)
     * - isLocked (trạng thái khóa)
     * - depositCode (mã nạp tiền)
     * - createdAt, updatedAt
     *
     * USE CASE:
     * - Admin dashboard: Xem tổng quan wallet system
     * - Monitor user balances
     * - Identify locked wallets
     *
     * @return List<WalletResponse> với tất cả wallets và user information
     */
    public List<WalletResponse> getAllWallets() {
        List<UserWallet> wallets = walletRepository.findAll();

        return wallets.stream()
                .map(wallet -> {
                    User user = wallet.getUser();
                    String depositCode = generateDepositCode(user);

                    return WalletResponse.builder()
                            .walletId(wallet.getId())
                            .userId(user.getUserId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .balance(wallet.getBalance())
                            .isLocked(wallet.isLocked())
                            .createdAt(wallet.getCreatedAt())
                            .updatedAt(wallet.getUpdatedAt())
                            .depositCode(depositCode)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert WalletTransaction entity → WalletTransactionResponse DTO
     *
     * Helper method để map entity sang DTO
     * Bao gồm tất cả fields cần thiết cho response
     *
     * @param transaction WalletTransaction entity
     * @return WalletTransactionResponse DTO
     */
    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction transaction) {
        return WalletTransactionResponse.builder()
                .transactionId(transaction.getId())
                .walletId(transaction.getWallet().getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .orderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null)
                .description(transaction.getDescription())
                .referenceNumber(transaction.getReferenceNumber())
                .performedBy(transaction.getPerformedBy())
                .note(transaction.getNote())
                .createdAt(transaction.getCreatedAt())
                .isCredit(transaction.isCredit())
                .isDebit(transaction.isDebit())
                .build();
    }

    /**
     * Tạo payment URL cho payment gateway
     *
     * Method này tạo URL để redirect user đến payment gateway
     *
     * ⚠️ PLACEHOLDER IMPLEMENTATION:
     * - Hiện tại đây là sandbox/test URLs
     * - Trong production, cần:
     *   1. Gọi API của payment gateway để create payment session
     *   2. Nhận payment URL thực từ gateway
     *   3. Return URL đó cho frontend
     *
     * PAYMENT GATEWAYS:
     *
     * 1. VNPay (Vietnam Payment Gateway):
     *    - Sandbox: https://sandbox.vnpayment.vn
     *    - Parameters: txnRef (transaction code), amount (VND x 100), returnUrl
     *    - Thực tế cần: vnp_TmnCode, vnp_HashSecret để sign request
     *
     * 2. MoMo (Mobile Money Vietnam):
     *    - Test: https://test-payment.momo.vn
     *    - Parameters: orderId, amount (VND), returnUrl
     *    - Thực tế cần: partnerCode, accessKey, secretKey để sign request
     *
     * 3. ZaloPay (Zalo Payment):
     *    - Sandbox: https://sandbox.zalopay.vn
     *    - Parameters: app_trans_id, amount (VND), redirect_url
     *    - Thực tế cần: app_id, key1, key2 để sign request
     *
     * 4. Bank Transfer (SMS Banking Integration):
     *    - Internal page: /payment/bank-transfer-instructions
     *    - Hiển thị hướng dẫn chuyển khoản và deposit code
     *    - User chuyển khoản → Bank gửi SMS → SMS forwarding app → Backend webhook
     *
     * RETURN URL:
     * - Sau khi thanh toán, gateway redirect user về returnUrl
     * - Frontend handle callback, hiển thị success/fail message
     *
     * @param gateway Payment gateway enum (VNPAY, MOMO, ZALOPAY, BANK_TRANSFER)
     * @param transactionCode Transaction code để tracking
     * @param amount Số tiền thanh toán (VND)
     * @param returnUrl URL để redirect sau khi thanh toán
     * @return Payment URL (sandbox/test hoặc production tùy implementation)
     * @throws IllegalArgumentException nếu gateway không được hỗ trợ
     */
    private String buildPaymentUrl(PaymentGateway gateway, String transactionCode,
                                   BigDecimal amount, String returnUrl) {
        // This is a placeholder. In production, you would:
        // 1. Call the payment gateway API to create payment session
        // 2. Return the actual payment URL from the gateway

        switch (gateway) {
            case VNPAY:
                return String.format("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?txnRef=%s&amount=%s&returnUrl=%s",
                        transactionCode, amount.multiply(BigDecimal.valueOf(100)).longValue(), returnUrl);
            case MOMO:
                return String.format("https://test-payment.momo.vn/v2/gateway/api/create?orderId=%s&amount=%s&returnUrl=%s",
                        transactionCode, amount.longValue(), returnUrl);
            case ZALOPAY:
                return String.format("https://sandbox.zalopay.vn/order?app_trans_id=%s&amount=%s&redirect_url=%s",
                        transactionCode, amount.longValue(), returnUrl);
            case BANK_TRANSFER:
                return "/payment/bank-transfer-instructions?txn=" + transactionCode;
            default:
                throw new IllegalArgumentException("Unsupported payment gateway: " + gateway);
        }
    }
}
