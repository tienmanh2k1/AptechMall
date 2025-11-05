package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.BankSms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BankSms entity
 */
@Repository
public interface BankSmsRepository extends JpaRepository<BankSms, Long> {

    /**
     * Find unprocessed SMS ordered by newest first
     * @param limit Maximum number of results
     * @return List of unprocessed SMS
     */
    @Query(value = "SELECT * FROM bank_sms WHERE processed = false ORDER BY received_at DESC LIMIT ?1",
           nativeQuery = true)
    List<BankSms> findUnprocessedSms(int limit);

    /**
     * Find top 10 unprocessed SMS
     */
    List<BankSms> findTop10ByProcessedFalseOrderByReceivedAtDesc();

    /**
     * Find SMS by transaction reference
     */
    Optional<BankSms> findByTransactionReference(String reference);

    /**
     * Check if SMS with transaction reference exists
     */
    boolean existsByTransactionReference(String reference);

    /**
     * Find all SMS received within a date range
     */
    List<BankSms> findByReceivedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find all processed SMS
     */
    List<BankSms> findByProcessedTrueOrderByProcessedAtDesc();

    /**
     * Find SMS that created deposits
     */
    List<BankSms> findByDepositCreatedTrueOrderByCreatedAtDesc();

    /**
     * Find SMS with errors
     */
    List<BankSms> findByErrorMessageIsNotNullOrderByCreatedAtDesc();
}
