package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.entity.BankSms;
import com.aptech.aptechMall.service.wallet.BankTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for bank transfer SMS webhook
 * Public endpoints for receiving bank SMS notifications
 * Base path: /api/bank-transfer
 */
@RestController
@RequestMapping("/api/bank-transfer")
@RequiredArgsConstructor
@Slf4j
public class BankTransferController {

    private final BankTransferService bankTransferService;

    /**
     * Webhook endpoint to receive bank SMS
     * POST /api/bank-transfer/sms-webhook (supports both query params and form data)
     * GET /api/bank-transfer/sms-webhook (for testing)
     *
     * @param sender SMS sender (bank identifier)
     * @param message SMS content
     * @param raw Raw SMS data (optional)
     * @param request HttpServletRequest to read body if params are null
     * @return Success response
     */
    @RequestMapping(value = "/sms-webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, Object>>> receiveSms(
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String raw,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // If parameters are null (POST body case), try to read from request body
            if (sender == null && message == null) {
                log.debug("Query params are null, attempting to read from request body");

                // Try to read JSON body
                try {
                    String contentType = request.getContentType();
                    log.debug("Content-Type: {}", contentType);

                    if (contentType != null && contentType.contains("application/json")) {
                        // Read JSON body
                        java.io.BufferedReader reader = request.getReader();
                        StringBuilder body = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            body.append(line);
                        }
                        log.debug("Request body: {}", body.toString());

                        // Parse JSON manually (simple parsing)
                        String bodyStr = body.toString();

                        // Try standard keys first
                        if (bodyStr.contains("sender")) {
                            sender = extractJsonValue(bodyStr, "sender");
                        }
                        if (bodyStr.contains("message")) {
                            message = extractJsonValue(bodyStr, "message");
                        }
                        if (bodyStr.contains("raw")) {
                            raw = extractJsonValue(bodyStr, "raw");
                        }

                        // Fallback to SMS Forwarder format: "from" -> sender, "content" -> message
                        if (sender == null && bodyStr.contains("from")) {
                            sender = extractJsonValue(bodyStr, "from");
                            log.debug("Extracted sender from 'from' field: {}", sender);
                        }
                        if (message == null && bodyStr.contains("content")) {
                            message = extractJsonValue(bodyStr, "content");
                            log.debug("Extracted message from 'content' field: {}", message);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to read request body: {}", e.getMessage());
                }
            }

            log.info("Received SMS webhook - sender: {}, message: {}", sender, message);

            // Save SMS to database
            BankSms savedSms = bankTransferService.saveSms(sender, message, raw);

            // Process SMS immediately
            bankTransferService.processSingleSms(savedSms);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "smsId", savedSms.getId(),
                    "processed", savedSms.isProcessed(),
                    "depositCreated", savedSms.isDepositCreated(),
                    "error", savedSms.getErrorMessage() != null ? savedSms.getErrorMessage() : ""
            );

            return ResponseEntity.ok(ApiResponse.success(response, "SMS received and processed"));

        } catch (Exception e) {
            log.error("Error receiving SMS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to process SMS", e.getMessage()));
        }
    }

    /**
     * Simple JSON value extractor (for basic JSON parsing)
     * @param json JSON string
     * @param key Key to extract
     * @return Value or null
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.warn("Failed to extract JSON value for key {}: {}", key, e.getMessage());
        }
        return null;
    }

    /**
     * Get unprocessed SMS and trigger processing
     * GET /api/bank-transfer/process-pending
     * @return List of unprocessed SMS
     */
    @GetMapping("/process-pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processPendingSms() {
        try {
            log.info("GET /api/bank-transfer/process-pending");

            List<BankSms> unprocessedSms = bankTransferService.getUnprocessedSms();
            int processedCount = bankTransferService.processUnprocessedSms();

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "unprocessedCount", unprocessedSms.size(),
                    "processedCount", processedCount,
                    "data", unprocessedSms
            );

            return ResponseEntity.ok(ApiResponse.success(response,
                    "Processed " + processedCount + " SMS"));

        } catch (Exception e) {
            log.error("Error processing pending SMS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to process pending SMS", e.getMessage()));
        }
    }

    /**
     * Get all SMS records
     * GET /api/bank-transfer/sms
     * @return List of all SMS
     */
    @GetMapping("/sms")
    public ResponseEntity<ApiResponse<List<BankSms>>> getAllSms() {
        try {
            log.info("GET /api/bank-transfer/sms");

            List<BankSms> smsList = bankTransferService.getAllSms();
            return ResponseEntity.ok(ApiResponse.success(smsList,
                    "Retrieved " + smsList.size() + " SMS records"));

        } catch (Exception e) {
            log.error("Error getting SMS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get SMS", e.getMessage()));
        }
    }

    /**
     * Get SMS by ID
     * GET /api/bank-transfer/sms/{id}
     * @param smsId SMS ID
     * @return BankSms details
     */
    @GetMapping("/sms/{id}")
    public ResponseEntity<ApiResponse<BankSms>> getSmsById(@PathVariable("id") Long smsId) {
        try {
            log.info("GET /api/bank-transfer/sms/{}", smsId);

            BankSms sms = bankTransferService.getSmsById(smsId);
            return ResponseEntity.ok(ApiResponse.success(sms, "SMS retrieved successfully"));

        } catch (RuntimeException e) {
            log.error("SMS not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting SMS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get SMS", e.getMessage()));
        }
    }

    /**
     * Get SMS with errors
     * GET /api/bank-transfer/sms/errors
     * @return List of SMS with errors
     */
    @GetMapping("/sms/errors")
    public ResponseEntity<ApiResponse<List<BankSms>>> getSmsWithErrors() {
        try {
            log.info("GET /api/bank-transfer/sms/errors");

            List<BankSms> smsWithErrors = bankTransferService.getSmsWithErrors();
            return ResponseEntity.ok(ApiResponse.success(smsWithErrors,
                    "Retrieved " + smsWithErrors.size() + " SMS with errors"));

        } catch (Exception e) {
            log.error("Error getting SMS with errors: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get SMS with errors", e.getMessage()));
        }
    }

    /**
     * Test endpoint to verify SMS webhook is working
     * GET /api/bank-transfer/test
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testWebhook() {
        return ResponseEntity.ok(ApiResponse.success(
                "Bank Transfer SMS Webhook is working!",
                "Webhook endpoint is accessible"));
    }
}
