package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.OrderStatus;
import com.aptech.aptechMall.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderStatusHistory entity
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /**
     * Find all status history for an order ordered by creation date
     * @param orderId Order ID
     * @return List of status history
     */
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /**
     * Find status history by order ID and status
     * @param orderId Order ID
     * @param status Order status
     * @return List of status history
     */
    List<OrderStatusHistory> findByOrderIdAndStatus(Long orderId, OrderStatus status);

    /**
     * Find latest status history for an order
     * @param orderId Order ID
     * @return List of status history (limited to 1)
     */
    List<OrderStatusHistory> findTop1ByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Count status history entries for an order
     * @param orderId Order ID
     * @return Number of entries
     */
    long countByOrderId(Long orderId);
}
