package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository interface for UserWallet entity
 */
@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    /**
     * Find wallet by user ID
     * Path: UserWallet.user.userId
     * @param userId User ID
     * @return Optional containing wallet
     */
    Optional<UserWallet> findByUserUserId(Long userId);

    /**
     * Check if wallet exists for user
     * Path: UserWallet.user.userId
     * @param userId User ID
     * @return true if wallet exists
     */
    boolean existsByUserUserId(Long userId);

    /**
     * Delete wallet by user ID
     * Path: UserWallet.user.userId
     * @param userId User ID
     */
    void deleteByUserUserId(Long userId);

    /**
     * Count wallets by locked status
     * @param isLocked Locked status
     * @return Number of wallets with the given locked status
     */
    long countByIsLocked(boolean isLocked);

    /**
     * Sum all wallet balances
     * @return Total balance across all wallets
     */
    @Query("SELECT SUM(w.balance) FROM UserWallet w")
    BigDecimal sumAllBalances();
}
