package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
