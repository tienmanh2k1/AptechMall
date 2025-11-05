package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.GatewayTransactionStatus;
import com.aptech.aptechMall.entity.PaymentGateway;
import com.aptech.aptechMall.entity.PaymentGatewayLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for PaymentGatewayLog entity
 */
@Repository
public interface PaymentGatewayLogRepository extends JpaRepository<PaymentGatewayLog, Long> {

    /**
     * Find log by transaction ID
     * @param transactionId Transaction ID
     * @return Optional containing log
     */
    Optional<PaymentGatewayLog> findByTransactionId(String transactionId);

    /**
     * Find log by gateway transaction ID
     * @param gatewayTransactionId Gateway transaction ID
     * @return Optional containing log
     */
    Optional<PaymentGatewayLog> findByGatewayTransactionId(String gatewayTransactionId);

    /**
     * Find all logs for a user
     * @param userId User ID
     * @param pageable Pagination info
     * @return Page of logs
     */
    Page<PaymentGatewayLog> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find logs by user and status
     * @param userId User ID
     * @param status Transaction status
     * @param pageable Pagination info
     * @return Page of logs
     */
    Page<PaymentGatewayLog> findByUserUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, GatewayTransactionStatus status, Pageable pageable);

    /**
     * Find logs by gateway and status
     * @param gateway Payment gateway
     * @param status Transaction status
     * @param pageable Pagination info
     * @return Page of logs
     */
    Page<PaymentGatewayLog> findByGatewayAndStatusOrderByCreatedAtDesc(
            PaymentGateway gateway, GatewayTransactionStatus status, Pageable pageable);
}
