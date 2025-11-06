package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.OrderAdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderAdditionalService entity
 */
@Repository
public interface OrderAdditionalServiceRepository extends JpaRepository<OrderAdditionalService, Long> {

    /**
     * Find all services for an order
     * @param orderId Order ID
     * @return List of order additional services
     */
    List<OrderAdditionalService> findByOrderId(Long orderId);

    /**
     * Find services by order ID and service ID
     * @param orderId Order ID
     * @param serviceId Service ID
     * @return List of order additional services
     */
    List<OrderAdditionalService> findByOrderIdAndServiceId(Long orderId, Long serviceId);

    /**
     * Count services for an order
     * @param orderId Order ID
     * @return Number of services
     */
    long countByOrderId(Long orderId);

    /**
     * Delete all services for an order
     * @param orderId Order ID
     */
    void deleteByOrderId(Long orderId);
}
