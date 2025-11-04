package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.Order;
import com.aptech.aptechMall.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by order number
     * @param orderNumber Order number
     * @return Optional containing Order if found
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find all orders for a user with pagination
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of Orders
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all orders for a user
     * @param userId User ID
     * @return List of Orders
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find orders by user ID and status
     * @param userId User ID
     * @param status Order status
     * @param pageable Pagination information
     * @return Page of Orders
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Find order by ID and user ID (for security check)
     * @param id Order ID
     * @param userId User ID
     * @return Optional containing Order if found
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Find order with items eagerly loaded
     * @param id Order ID
     * @return Optional containing Order if found
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /**
     * Count orders by user ID
     * @param userId User ID
     * @return Number of orders
     */
    long countByUserId(Long userId);

    /**
     * Check if order number exists
     * @param orderNumber Order number
     * @return true if exists
     */
    boolean existsByOrderNumber(String orderNumber);
}
