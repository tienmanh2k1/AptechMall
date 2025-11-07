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
 * Controller quản lý ví điện tử (E-Wallet)
 *
 * Endpoint base: /api/wallet
 * YÊU CẦU AUTHENTICATION: Tất cả endpoints cần JWT token
 *
 * Hệ thống ví điện tử cho phép:
 * - Xem số dư ví
 * - Nạp tiền vào ví (qua VNPay, MoMo, ZaloPay, Bank Transfer)
 * - Xem lịch sử giao dịch
 * - Admin: Khóa/mở khóa ví, xem tất cả ví
 *
 * Bảo mật:
 * - userId lấy từ JWT token (KHÔNG chấp nhận từ client)
 * - Admin operations cần role ADMIN
 * - Ví có thể bị khóa (locked) để ngăn giao dịch bất thường
 *
 * Loại giao dịch:
 * - DEPOSIT: Nạp tiền vào ví
 * - WITHDRAWAL: Rút tiền từ ví
 * - ORDER_PAYMENT: Thanh toán đơn hàng
 * - ORDER_REFUND: Hoàn tiền khi hủy đơn
 * - ADMIN_ADJUSTMENT: Admin điều chỉnh số dư thủ công
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    /**
     * Lấy thông tin ví của user hiện tại
     *
     * GET /api/wallet
     *
     * Response bao gồm:
     * - walletId: ID của ví
     * - userId: ID người dùng
     * - balance: Số dư hiện tại (VND)
     * - isLocked: Trạng thái khóa ví
     * - createdAt, updatedAt: Thời gian tạo/cập nhật
     *
     * @return WalletResponse chứa thông tin ví
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
     * Khởi tạo giao dịch nạp tiền vào ví
     *
     * POST /api/wallet/deposit/initiate
     *
     * Quy trình nạp tiền:
     * 1. User gọi endpoint này với số tiền và cổng thanh toán
     * 2. Hệ thống tạo transaction PENDING
     * 3. Trả về paymentUrl để redirect user đến cổng thanh toán
     * 4. User thanh toán tại cổng (VNPay/MoMo/ZaloPay/BankTransfer)
     * 5. Cổng thanh toán gọi callback endpoint để xác nhận
     * 6. Hệ thống cập nhật số dư ví
     *
     * Request body:
     * - amount: Số tiền nạp (VND), tối thiểu 10,000 VND
     * - paymentGateway: VNPAY / MOMO / ZALOPAY / BANK_TRANSFER
     *
     * @param request Thông tin nạp tiền
     * @return DepositInitiateResponse với paymentUrl để redirect
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
     * Xử lý callback từ cổng thanh toán sau khi nạp tiền thành công
     *
     * POST /api/wallet/deposit/callback
     *
     * Endpoint này được GỌI BỞI CỔNG THANH TOÁN (VNPay/MoMo/ZaloPay) sau khi user thanh toán thành công.
     * Không nên gọi trực tiếp từ frontend.
     *
     * Xử lý:
     * - Kiểm tra transaction tồn tại và ở trạng thái PENDING
     * - Xác thực referenceNumber từ cổng thanh toán
     * - Cập nhật trạng thái transaction thành COMPLETED
     * - Cộng tiền vào ví user
     * - Ghi log vào PaymentGatewayLog
     *
     * @param amount Số tiền đã thanh toán
     * @param paymentGateway Cổng thanh toán đã sử dụng
     * @param referenceNumber Mã tham chiếu từ cổng thanh toán
     * @return WalletTransactionResponse với thông tin giao dịch đã hoàn thành
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
     * Lấy lịch sử giao dịch ví với các bộ lọc
     *
     * GET /api/wallet/transactions
     *
     * Hỗ trợ lọc theo:
     * - transactionType: DEPOSIT / WITHDRAWAL / ORDER_PAYMENT / ORDER_REFUND / ADMIN_ADJUSTMENT
     * - startDate, endDate: Khoảng thời gian
     * - page, size: Phân trang (mặc định: page=0, size=20)
     * - sort: Sắp xếp (createdAt,desc mặc định)
     *
     * Response là Page object bao gồm:
     * - content: Danh sách giao dịch
     * - totalElements: Tổng số giao dịch
     * - totalPages: Tổng số trang
     * - pageable: Thông tin phân trang
     *
     * @param filter Bộ lọc giao dịch (query parameters)
     * @return Page<WalletTransactionResponse> danh sách giao dịch đã lọc
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
     * Lấy thông tin chi tiết của một giao dịch cụ thể
     *
     * GET /api/wallet/transactions/{id}
     *
     * Bảo mật: Chỉ cho phép xem giao dịch của chính mình
     *
     * @param transactionId ID của giao dịch cần xem
     * @return WalletTransactionResponse với thông tin chi tiết giao dịch
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
     * Khóa ví của một user (chỉ ADMIN)
     *
     * POST /api/wallet/{userId}/lock
     * YÊU CẦU: Role ADMIN
     *
     * Khi ví bị khóa:
     * - User KHÔNG THỂ nạp tiền
     * - User KHÔNG THỂ sử dụng ví để thanh toán
     * - Chỉ có thể xem số dư
     *
     * Mục đích:
     * - Ngăn chặn giao dịch bất thường
     * - Tạm khóa tài khoản nghi ngờ gian lận
     * - Bảo vệ hệ thống khỏi lạm dụng
     *
     * @param targetUserId ID của user cần khóa ví
     * @return Thông báo thành công
     */
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')") // Spring Security kiểm tra role
    public ResponseEntity<ApiResponse<String>> lockWallet(@PathVariable("userId") Long targetUserId) {
        try {
            log.info("POST /api/wallet/{}/lock - Admin action", targetUserId);

            walletService.lockWallet(targetUserId);
            return ResponseEntity.ok(ApiResponse.success(null, "Wallet locked successfully"));

        } catch (Exception e) {
            log.error("Error locking wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to lock wallet", e.getMessage()));
        }
    }

    /**
     * Mở khóa ví của một user (chỉ ADMIN)
     *
     * POST /api/wallet/{userId}/unlock
     * YÊU CẦU: Role ADMIN
     *
     * Sau khi mở khóa, user có thể sử dụng ví bình thường trở lại
     *
     * @param targetUserId ID của user cần mở khóa ví
     * @return Thông báo thành công
     */
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> unlockWallet(@PathVariable("userId") Long targetUserId) {
        try {
            log.info("POST /api/wallet/{}/unlock - Admin action", targetUserId);

            walletService.unlockWallet(targetUserId);
            return ResponseEntity.ok(ApiResponse.success(null, "Wallet unlocked successfully"));

        } catch (Exception e) {
            log.error("Error unlocking wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to unlock wallet", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách tất cả ví trong hệ thống (chỉ ADMIN)
     *
     * GET /api/wallet/admin/all
     * YÊU CẦU: Role ADMIN
     *
     * Trả về tất cả ví của mọi user để admin theo dõi
     *
     * @return List<WalletResponse> danh sách tất cả ví
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getAllWallets() {
        try {
            log.info("GET /api/wallet/admin/all - Admin action");

            List<WalletResponse> wallets = walletService.getAllWallets();
            return ResponseEntity.ok(ApiResponse.success(wallets, "Wallets retrieved successfully"));

        } catch (Exception e) {
            log.error("Error getting all wallets: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get wallets", e.getMessage()));
        }
    }
}
