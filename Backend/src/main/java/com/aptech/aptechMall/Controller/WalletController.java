package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.dto.wallet.*;
import com.aptech.aptechMall.entity.PaymentGateway;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.service.wallet.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for wallet operations
 * All endpoints require authentication
 * Base path: /api/wallet
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    /**
     * Get current user's wallet information
     * GET /api/wallet
     * @return WalletResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet() {
        try {
            Long userId = AuthenticationUtil.getCurrentUserId();
            log.info("GET /api/wallet - userId: {}", userId);

            WalletResponse wallet = walletService.getWallet(userId);
            return ResponseEntity.ok(ApiResponse.success(wallet, "Wallet retrieved successfully"));

        } catch (IllegalStateException e) {
            log.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get wallet", e.getMessage()));
        }
    }

    /**
     * Initiate deposit transaction
     * POST /api/wallet/deposit/initiate
     * @param request Deposit request
     * @return DepositInitiateResponse with payment URL
     */
    @PostMapping("/deposit/initiate")
    public ResponseEntity<ApiResponse<DepositInitiateResponse>> initiateDeposit(
            @Valid @RequestBody DepositRequest request) {
        try {
            Long userId = AuthenticationUtil.getCurrentUserId();
            log.info("POST /api/wallet/deposit/initiate - userId: {}, amount: {}, gateway: {}",
                    userId, request.getAmount(), request.getPaymentGateway());

            DepositInitiateResponse response = walletService.initiateDeposit(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Deposit initiated successfully"));

        } catch (IllegalStateException e) {
            log.error("Error initiating deposit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error initiating deposit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to initiate deposit", e.getMessage()));
        }
    }

    /**
     * Process deposit callback from payment gateway
     * POST /api/wallet/deposit/callback
     * This endpoint is called by payment gateway after successful payment
     * @param amount Deposit amount
     * @param paymentGateway Payment gateway used
     * @param referenceNumber Payment reference from gateway
     * @return WalletTransactionResponse
     */
    @PostMapping("/deposit/callback")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> processDepositCallback(
            @RequestParam BigDecimal amount,
            @RequestParam PaymentGateway paymentGateway,
            @RequestParam String referenceNumber) {
        try {
            Long userId = AuthenticationUtil.getCurrentUserId();
            log.info("POST /api/wallet/deposit/callback - userId: {}, amount: {}, gateway: {}, ref: {}",
                    userId, amount, paymentGateway, referenceNumber);

            WalletTransactionResponse transaction = walletService.processDeposit(
                    userId, amount, paymentGateway, referenceNumber);

            return ResponseEntity.ok(ApiResponse.success(transaction, "Deposit processed successfully"));

        } catch (IllegalStateException e) {
            log.error("Error processing deposit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error processing deposit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to process deposit", e.getMessage()));
        }
    }

    /**
     * Get transaction history with filters
     * GET /api/wallet/transactions
     * @param filter Transaction filter (query parameters)
     * @return Page of WalletTransactionResponse
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> getTransactionHistory(
            @ModelAttribute TransactionFilterRequest filter) {
        try {
            Long userId = AuthenticationUtil.getCurrentUserId();
            log.info("GET /api/wallet/transactions - userId: {}, filter: {}", userId, filter);

            Page<WalletTransactionResponse> transactions = walletService.getTransactionHistory(userId, filter);
            return ResponseEntity.ok(ApiResponse.success(transactions, "Transactions retrieved successfully"));

        } catch (IllegalStateException e) {
            log.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get transactions", e.getMessage()));
        }
    }

    /**
     * Get single transaction by ID
     * GET /api/wallet/transactions/{id}
     * @param transactionId Transaction ID
     * @return WalletTransactionResponse
     */
    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> getTransaction(
            @PathVariable("id") Long transactionId) {
        try {
            Long userId = AuthenticationUtil.getCurrentUserId();
            log.info("GET /api/wallet/transactions/{} - userId: {}", transactionId, userId);

            WalletTransactionResponse transaction = walletService.getTransaction(userId, transactionId);
            return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction retrieved successfully"));

        } catch (IllegalStateException e) {
            log.error("Error getting transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage(), null));
        } catch (RuntimeException e) {
            log.error("Transaction not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get transaction", e.getMessage()));
        }
    }

    /**
     * Lock user wallet (admin operation)
     * POST /api/wallet/{userId}/lock
     * @param targetUserId User ID to lock wallet
     * @return Success message
     */
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> lockWallet(@PathVariable("userId") Long targetUserId) {
        try {
            // TODO: Add admin authorization check
            log.info("POST /api/wallet/{}/lock", targetUserId);

            walletService.lockWallet(targetUserId);
            return ResponseEntity.ok(ApiResponse.success(null, "Wallet locked successfully"));

        } catch (Exception e) {
            log.error("Error locking wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to lock wallet", e.getMessage()));
        }
    }

    /**
     * Unlock user wallet (admin operation)
     * POST /api/wallet/{userId}/unlock
     * @param targetUserId User ID to unlock wallet
     * @return Success message
     */
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> unlockWallet(@PathVariable("userId") Long targetUserId) {
        try {
            // TODO: Add admin authorization check
            log.info("POST /api/wallet/{}/unlock", targetUserId);

            walletService.unlockWallet(targetUserId);
            return ResponseEntity.ok(ApiResponse.success(null, "Wallet unlocked successfully"));

        } catch (Exception e) {
            log.error("Error unlocking wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to unlock wallet", e.getMessage()));
        }
    }

    /**
     * Get all wallets (admin operation)
     * GET /api/wallet/admin/all
     * @return List of all wallets
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getAllWallets() {
        try {
            log.info("GET /api/wallet/admin/all");

            List<WalletResponse> wallets = walletService.getAllWallets();
            return ResponseEntity.ok(ApiResponse.success(wallets, "Wallets retrieved successfully"));

        } catch (Exception e) {
            log.error("Error getting all wallets: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get wallets", e.getMessage()));
        }
    }
}
