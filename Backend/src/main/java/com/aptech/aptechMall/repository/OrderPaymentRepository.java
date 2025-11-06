package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.OrderPayment;
import com.aptech.aptechMall.entity.PaymentStatus;
import com.aptech.aptechMall.entity.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrderPayment entity
 */
@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    /**
     * Find all payments for a specific order
     * @param orderId Order ID
     * @return List of payments
     */
    List<OrderPayment> findByOrderId(Long orderId);

    /**
     * Find payments by order ID and payment type
     * @param orderId Order ID
     * @param paymentType Payment type
     * @return List of payments
     */
    List<OrderPayment> findByOrderIdAndPaymentType(Long orderId, PaymentType paymentType);

    /**
     * Find payments by order ID and payment status
     * @param orderId Order ID
     * @param paymentStatus Payment status
     * @return List of payments
     */
    List<OrderPayment> findByOrderIdAndPaymentStatus(Long orderId, PaymentStatus paymentStatus);

    /**
     * Find deposit payment for an order
     * @param orderId Order ID
     * @param paymentType Payment type (DEPOSIT)
     * @return Optional containing deposit payment
     */
    Optional<OrderPayment> findFirstByOrderIdAndPaymentType(Long orderId, PaymentType paymentType);

    /**
     * Check if deposit payment exists and is completed
     * @param orderId Order ID
     * @param paymentType Payment type
     * @param paymentStatus Payment status
     * @return true if exists
     */
    boolean existsByOrderIdAndPaymentTypeAndPaymentStatus(Long orderId, PaymentType paymentType, PaymentStatus paymentStatus);
}
