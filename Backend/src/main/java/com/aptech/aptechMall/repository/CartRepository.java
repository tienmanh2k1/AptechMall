package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Cart entity
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find cart by user ID with items eagerly loaded
     * @param userId User ID
     * @return Optional containing Cart if found
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    /**
     * Find cart by user ID
     * @param userId User ID
     * @return Optional containing Cart if found
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * Check if cart exists for user
     * @param userId User ID
     * @return true if exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete cart by user ID
     * @param userId User ID
     */
    void deleteByUserId(Long userId);
}
