package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.TransactionType;
import com.aptech.aptechMall.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for WalletTransaction entity
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Find all transactions for a wallet
     * @param walletId Wallet ID
     * @param pageable Pagination info
     * @return Page of transactions
     */
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Find transactions by wallet ID and transaction type
     * @param walletId Wallet ID
     * @param transactionType Transaction type
     * @param pageable Pagination info
     * @return Page of transactions
     */
    Page<WalletTransaction> findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
            Long walletId, TransactionType transactionType, Pageable pageable);

    /**
     * Find transactions by order ID
     * @param orderId Order ID
     * @return List of transactions
     */
    List<WalletTransaction> findByOrderId(Long orderId);

    /**
     * Find transactions within date range
     * @param walletId Wallet ID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination info
     * @return Page of transactions
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.id = :walletId " +
           "AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
