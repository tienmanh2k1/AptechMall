package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.SystemFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for SystemFeeConfig entity
 */
@Repository
public interface SystemFeeConfigRepository extends JpaRepository<SystemFeeConfig, Long> {

    /**
     * Find the active fee configuration
     * @return Optional containing active fee config
     */
    Optional<SystemFeeConfig> findByIsActiveTrue();

    /**
     * Check if any active configuration exists
     * @return true if active config exists
     */
    boolean existsByIsActiveTrue();
}
