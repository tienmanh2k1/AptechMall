package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.AdditionalService;
import com.aptech.aptechMall.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AdditionalService entity
 */
@Repository
public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {

    /**
     * Find all active services ordered by display order
     * @return List of active services
     */
    List<AdditionalService> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find service by type
     * @param serviceType Service type
     * @return Optional containing service
     */
    Optional<AdditionalService> findByServiceType(ServiceType serviceType);

    /**
     * Find all services by type and active status
     * @param serviceType Service type
     * @param isActive Active status
     * @return List of services
     */
    List<AdditionalService> findByServiceTypeAndIsActive(ServiceType serviceType, boolean isActive);

    /**
     * Check if service exists and is active
     * @param id Service ID
     * @return true if service exists and is active
     */
    boolean existsByIdAndIsActiveTrue(Long id);
}
