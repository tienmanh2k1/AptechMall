package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderItem entity
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items for a specific order
     * @param orderId Order ID
     * @return List of OrderItems
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Delete all items for a specific order
     * @param orderId Order ID
     */
    void deleteByOrderId(Long orderId);

    /**
     * Count items in an order
     * @param orderId Order ID
     * @return Number of items
     */
    long countByOrderId(Long orderId);
}
