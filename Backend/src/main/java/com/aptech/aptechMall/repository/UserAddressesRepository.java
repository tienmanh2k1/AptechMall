package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.model.jpa.UserAddresses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserAddresses entity
 */
@Repository
public interface UserAddressesRepository extends JpaRepository<UserAddresses, Long> {

    /**
     * Find all addresses for a user
     * @param userId User ID
     * @return List of addresses
     */
    List<UserAddresses> findByUserUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    /**
     * Find default address for a user
     * @param userId User ID
     * @return Optional containing default address
     */
    Optional<UserAddresses> findByUserUserIdAndIsDefaultTrue(Long userId);

    /**
     * Unset all default addresses for a user
     * @param userId User ID
     */
    @Modifying
    @Query("UPDATE UserAddresses ua SET ua.isDefault = false WHERE ua.user.userId = :userId")
    void unsetAllDefaultForUser(@Param("userId") Long userId);

    /**
     * Count addresses for a user
     * @param userId User ID
     * @return Number of addresses
     */
    long countByUserUserId(Long userId);

    /**
     * Check if address exists for user
     * @param addressId Address ID
     * @param userId User ID
     * @return true if address belongs to user
     */
    boolean existsByIdAndUserUserId(Long addressId, Long userId);
}
