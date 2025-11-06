package com.aptech.aptechMall.service.wallet;

import com.aptech.aptechMall.entity.BankSms;
import com.aptech.aptechMall.entity.PaymentGateway;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.BankSmsRepository;
import com.aptech.aptechMall.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for processing bank transfer SMS notifications
 * Handles automatic deposit creation from bank SMS
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransferService {

    private final BankSmsRepository smsRepository;
    private final BankSmsParserService parserService;
    private final WalletService walletService;
    private final UserRepository userRepository;

    /**
     * Save incoming SMS to database
     * @param sender SMS sender (bank identifier)
     * @param message SMS content
     * @param raw Raw SMS data
     * @return Saved BankSms entity
     */
    @Transactional
    public BankSms saveSms(String sender, String message, String raw) {
        BankSms sms = BankSms.builder()
                .sender(sender)
                .message(message)
                .raw(raw)
                .receivedAt(LocalDateTime.now())
                .processed(false)
                .depositCreated(false)
                .build();

        BankSms savedSms = smsRepository.save(sms);
        log.info("Saved SMS from {}: id={}", sender, savedSms.getId());

        return savedSms;
    }

    /**
     * Process unprocessed SMS and create deposits
     * @return Number of SMS processed
     */
    @Transactional
    public int processUnprocessedSms() {
        List<BankSms> unprocessedSms = smsRepository.findTop10ByProcessedFalseOrderByReceivedAtDesc();

        if (unprocessedSms.isEmpty()) {
            log.debug("No unprocessed SMS found");
            return 0;
        }

        log.info("Found {} unprocessed SMS to process", unprocessedSms.size());
        int processedCount = 0;

        for (BankSms sms : unprocessedSms) {
            try {
                processSingleSms(sms);
                processedCount++;
            } catch (Exception e) {
                log.error("Error processing SMS id={}: {}", sms.getId(), e.getMessage(), e);
                sms.setError("Processing error: " + e.getMessage());
                sms.markAsProcessed();
                smsRepository.save(sms);
            }
        }

        log.info("Processed {} SMS successfully", processedCount);
        return processedCount;
    }

    /**
     * Process a single SMS message
     * @param sms BankSms to process
     */
    @Transactional
    public void processSingleSms(BankSms sms) {
        log.info("Processing SMS id={}", sms.getId());

        // Check if already processed
        if (sms.isProcessed()) {
            log.warn("SMS id={} already processed, skipping", sms.getId());
            return;
        }

        // Parse SMS to extract transaction info
        boolean parseSuccess = parserService.parseSms(sms);
        if (!parseSuccess) {
            log.error("Failed to parse SMS id={}: {}", sms.getId(), sms.getErrorMessage());
            sms.markAsProcessed();
            smsRepository.save(sms);
            return;
        }

        // Validate extracted data
        if (sms.getParsedAmount() == null || sms.getParsedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            sms.setError("Invalid amount: " + sms.getParsedAmount());
            sms.markAsProcessed();
            smsRepository.save(sms);
            return;
        }

        // Check if transaction reference already exists (excluding current SMS)
        if (sms.getTransactionReference() != null) {
            Optional<BankSms> existingSms = smsRepository.findByTransactionReference(sms.getTransactionReference());
            if (existingSms.isPresent() && !existingSms.get().getId().equals(sms.getId())) {
                log.warn("Duplicate transaction reference: {} (found in SMS id={})",
                        sms.getTransactionReference(), existingSms.get().getId());
                sms.setError("Duplicate transaction reference");
                sms.markAsProcessed();
                smsRepository.save(sms);
                return;
            }
        }

        // Create deposit if username, userId, or email is found
        if (sms.getExtractedUsername() != null || sms.getExtractedUserId() != null || sms.getExtractedEmail() != null) {
            try {
                createDepositFromSms(sms);
            } catch (Exception e) {
                log.error("Failed to create deposit from SMS id={}: {}", sms.getId(), e.getMessage(), e);
                sms.setError("Deposit creation failed: " + e.getMessage());
            }
        } else {
            log.warn("No username, user ID, or email found in SMS id={}, cannot create deposit", sms.getId());
            sms.setError("No user identifier found in SMS content");
        }

        // Mark as processed
        sms.markAsProcessed();
        smsRepository.save(sms);
    }

    /**
     * Create wallet deposit from parsed SMS
     * Tries multiple methods: Username -> UserId -> Email (with fallback)
     * @param sms Parsed BankSms
     */
    private void createDepositFromSms(BankSms sms) {
        BigDecimal amount = sms.getParsedAmount();
        String reference = sms.getTransactionReference();

        // Find user by trying username, userId, or email (with fallback)
        User user = null;
        String identifier = null;

        // Priority 1: Try Username (no special chars)
        if (sms.getExtractedUsername() != null) {
            identifier = sms.getExtractedUsername();
            Optional<User> userOpt = userRepository.findByUsername(sms.getExtractedUsername());
            if (userOpt.isPresent()) {
                user = userOpt.get();
                log.info("✅ Found user by username: {} -> userId={}", sms.getExtractedUsername(), user.getUserId());
            } else {
                log.warn("⚠️ Username '{}' not found, will try userId fallback", sms.getExtractedUsername());
            }
        }

        // Priority 2: Try UserId (if username not found or not provided)
        if (user == null && sms.getExtractedUserId() != null) {
            identifier = "USER" + sms.getExtractedUserId();
            Optional<User> userOpt = userRepository.findById(sms.getExtractedUserId());
            if (userOpt.isPresent()) {
                user = userOpt.get();
                log.info("✅ Found user by ID: {} -> username={}", sms.getExtractedUserId(), user.getUsername());
            } else {
                log.warn("⚠️ UserId {} not found, will try email fallback", sms.getExtractedUserId());
            }
        }

        // Priority 3: Try Email (deprecated - has special chars)
        if (user == null && sms.getExtractedEmail() != null) {
            identifier = sms.getExtractedEmail();
            Optional<User> userOpt = userRepository.findByEmail(sms.getExtractedEmail());
            if (userOpt.isPresent()) {
                user = userOpt.get();
                log.warn("✅ Found user by email (deprecated): {} -> userId={}", sms.getExtractedEmail(), user.getUserId());
            } else {
                log.error("❌ Email '{}' not found", sms.getExtractedEmail());
            }
        }

        // If no user found after all attempts, throw error
        if (user == null) {
            String errorMsg = String.format(
                "User not found. Tried: username=%s, userId=%s, email=%s",
                sms.getExtractedUsername(),
                sms.getExtractedUserId(),
                sms.getExtractedEmail()
            );
            throw new RuntimeException(errorMsg);
        }

        Long userId = user.getUserId();
        log.info("Creating deposit for user {} ({}): amount={}, ref={}", userId, identifier, amount, reference);

        // Call WalletService to process deposit
        var transaction = walletService.processDeposit(
                userId,
                amount,
                PaymentGateway.BANK_TRANSFER,
                reference
        );

        // Mark deposit as created
        sms.markDepositCreated(transaction.getTransactionId());
        smsRepository.save(sms);

        log.info("Successfully created deposit: transactionId={}, userId={}, identifier={}, amount={}",
                 transaction.getTransactionId(), userId, identifier, amount);
    }

    /**
     * Get all unprocessed SMS
     */
    public List<BankSms> getUnprocessedSms() {
        return smsRepository.findTop10ByProcessedFalseOrderByReceivedAtDesc();
    }

    /**
     * Get SMS by ID
     */
    public BankSms getSmsById(Long id) {
        return smsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SMS not found with id: " + id));
    }

    /**
     * Get all SMS
     */
    public List<BankSms> getAllSms() {
        return smsRepository.findAll();
    }

    /**
     * Get SMS with errors
     */
    public List<BankSms> getSmsWithErrors() {
        return smsRepository.findByErrorMessageIsNotNullOrderByCreatedAtDesc();
    }
}
