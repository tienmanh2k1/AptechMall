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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing user wallet and transactions
 * Handles deposit, withdrawal, and transaction history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Get or create wallet for user
     * @param userId User ID
     * @return UserWallet entity
     */
    @Transactional
    public UserWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserUserId(userId)
                .orElseGet(() -> {
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
                });
    }

    /**
     * Get wallet information
     * @param userId User ID
     * @return WalletResponse
     */
    public WalletResponse getWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);

        return WalletResponse.builder()
                .walletId(wallet.getId())
                .userId(userId)
                .balance(wallet.getBalance())
                .isLocked(wallet.isLocked())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    /**
     * Initiate deposit transaction
     * Creates a pending transaction and returns payment URL
     * @param userId User ID
     * @param request Deposit request
     * @return DepositInitiateResponse with payment URL
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
     * Process deposit after successful payment
     * Called by payment gateway callback
     * @param userId User ID
     * @param amount Deposit amount
     * @param paymentGateway Payment gateway used
     * @param referenceNumber Payment reference from gateway
     * @return WalletTransactionResponse
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
     * Get transaction history with filters
     * @param userId User ID
     * @param filter Transaction filter request
     * @return Page of WalletTransactionResponse
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
     * Get single transaction by ID
     * Verifies transaction belongs to user
     * @param userId User ID
     * @param transactionId Transaction ID
     * @return WalletTransactionResponse
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
     * Lock wallet (admin operation)
     * @param userId User ID
     */
    @Transactional
    public void lockWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.lock();
        walletRepository.save(wallet);
        log.info("Locked wallet for user {}", userId);
    }

    /**
     * Unlock wallet (admin operation)
     * @param userId User ID
     */
    @Transactional
    public void unlockWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.unlock();
        walletRepository.save(wallet);
        log.info("Unlocked wallet for user {}", userId);
    }

    /**
     * Map WalletTransaction entity to response DTO
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
     * Build payment URL for payment gateway
     * TODO: Implement actual payment gateway integration
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
