package com.aptech.aptechMall.service.wallet;

import com.aptech.aptechMall.entity.BankSms;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing bank SMS messages
 * Extracts transaction information from SMS text
 */
@Service
@Slf4j
public class BankSmsParserService {

    /**
     * Parse SMS message and extract transaction information
     *
     * Supported formats:
     * 1. MBBank: "TK 09xxx279 GD: +200,000VND 05/11/25 07:21 SD: 335,163VND ND: MBVCB.11586295199.373933.TRIEU THIHONG THAO chuyen tien USER123"
     * 2. Vietcombank: "TK 1234567890 +500,000 VND. GD: 123456. ND: NAP TIEN USER123"
     * 3. Generic: "+500000d GD:123456 ND:NAPTIEN USER123"
     * 4. Simple: "Tai khoan +500k USER123"
     * 5. Test format: "GD 100k USER1"
     *
     * IMPORTANT: Transfer content MUST include USER{id} anywhere in the message
     * Examples: "USER123", "NAP TIEN USER123", "chuyen tien USER1"
     *
     * @param sms BankSms entity to parse
     * @return true if parsing successful, false otherwise
     */
    public boolean parseSms(BankSms sms) {
        if (sms == null || sms.getMessage() == null || sms.getMessage().trim().isEmpty()) {
            log.warn("Empty SMS message, cannot parse");
            return false;
        }

        String message = sms.getMessage().toUpperCase().trim();
        log.info("Parsing SMS: {}", message);

        try {
            // Extract amount
            BigDecimal amount = extractAmount(message);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                sms.setError("Cannot extract valid amount from SMS");
                return false;
            }
            sms.setParsedAmount(amount);

            // Extract transaction reference (GD number)
            String reference = extractTransactionReference(message);
            sms.setTransactionReference(reference);

            // Extract username/deposit code (preferred - no special chars)
            String username = extractUsername(message);
            sms.setExtractedUsername(username);

            // Extract user ID from content (backward compatibility)
            Long userId = extractUserId(message);
            sms.setExtractedUserId(userId);

            // Extract email from content (deprecated - has special chars)
            String email = extractEmail(message);
            sms.setExtractedEmail(email);

            log.info("Parsed SMS - Amount: {}, Reference: {}, Username: {}, UserId: {}, Email: {}",
                     amount, reference, username, userId, email);
            return true;

        } catch (Exception e) {
            log.error("Error parsing SMS: {}", e.getMessage(), e);
            sms.setError("Parse error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract amount from SMS message
     *
     * Patterns (in priority order):
     * - +500,000 VND or +500,000VND (with + sign, highest priority)
     * - +500k or +500K (with + sign)
     * - 500,000 VND (without +, but 6+ digits)
     * - GD 100k (test format)
     */
    private BigDecimal extractAmount(String message) {
        // Pattern 1: +500,000 VND or +500,000VND (MUST have + sign)
        // This ensures we match transaction amount, not account number
        Pattern pattern1 = Pattern.compile("[+]([0-9,]+)\\s*(?:VND|D|Đ)?");
        Matcher matcher1 = pattern1.matcher(message);
        if (matcher1.find()) {
            String amountStr = matcher1.group(1).replace(",", "");
            try {
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse amount with +: {}", matcher1.group(1));
            }
        }

        // Pattern 2: +500k or +500K (MUST have + sign and K suffix)
        Pattern pattern2 = Pattern.compile("[+]([0-9]+)K");
        Matcher matcher2 = pattern2.matcher(message);
        if (matcher2.find()) {
            String amountStr = matcher2.group(1);
            try {
                return new BigDecimal(amountStr).multiply(new BigDecimal("1000"));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse K amount: {}", matcher2.group(1));
            }
        }

        // Pattern 3: Large number without + (6+ digits with commas, e.g., "500,000 VND")
        // This avoids matching short numbers like "09" from account numbers
        Pattern pattern3 = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+)\\s*(?:VND|D|Đ)");
        Matcher matcher3 = pattern3.matcher(message);
        if (matcher3.find()) {
            String amountStr = matcher3.group(1).replace(",", "");
            try {
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse large amount: {}", matcher3.group(1));
            }
        }

        // Pattern 4: K suffix without + (500K) - lower priority
        Pattern pattern4 = Pattern.compile("\\b([0-9]+)K");
        Matcher matcher4 = pattern4.matcher(message);
        if (matcher4.find()) {
            String amountStr = matcher4.group(1);
            try {
                return new BigDecimal(amountStr).multiply(new BigDecimal("1000"));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse K amount without +: {}", matcher4.group(1));
            }
        }

        // Pattern 5: Simple "GD 100" format (for testing only)
        Pattern pattern5 = Pattern.compile("^GD\\s+([0-9]+)$");
        Matcher matcher5 = pattern5.matcher(message);
        if (matcher5.find()) {
            String amountStr = matcher5.group(1);
            try {
                return new BigDecimal(amountStr).multiply(new BigDecimal("1000"));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse GD amount: {}", matcher5.group(1));
            }
        }

        log.warn("No amount pattern matched for message: {}", message);
        return null;
    }

    /**
     * Extract transaction reference (GD number)
     *
     * Patterns:
     * - MBVCB.11586295199.373933 (MBBank format)
     * - Ma GD: 123456 or Ma GD 123456
     * - GD: 123456 (after ND: field, avoid matching "GD: +200,000VND")
     * - GD:123456
     * - GD 123456
     */
    private String extractTransactionReference(String message) {
        // Pattern 1: MBVCB format (MBBank) - full code
        Pattern mbvcbPattern = Pattern.compile("MBVCB\\.[0-9]+\\.[0-9]+");
        Matcher mbvcbMatcher = mbvcbPattern.matcher(message);
        if (mbvcbMatcher.find()) {
            return mbvcbMatcher.group(0).replace(".", "_");
        }

        // Pattern 2: MBVCB short format - extract last number only
        Pattern mbvcbShortPattern = Pattern.compile("MBVCB\\.[0-9]+\\.([0-9]+)");
        Matcher mbvcbShortMatcher = mbvcbShortPattern.matcher(message);
        if (mbvcbShortMatcher.find()) {
            return "MBVCB_" + mbvcbShortMatcher.group(1);
        }

        // Pattern 3: "Ma GD" at end of SMS (some banks)
        Pattern maGdPattern = Pattern.compile("MA\\s+GD[:\\s]*([0-9A-Z]+)");
        Matcher maGdMatcher = maGdPattern.matcher(message);
        if (maGdMatcher.find()) {
            return "GD" + maGdMatcher.group(1);
        }

        // Pattern 4: Standard "GD:" after ND: field (avoid "GD: +200,000VND")
        // Only match if followed by non-currency characters
        Pattern gdAfterNdPattern = Pattern.compile("ND:.*GD[:\\s]*([0-9A-Z]{5,})");
        Matcher gdAfterNdMatcher = gdAfterNdPattern.matcher(message);
        if (gdAfterNdMatcher.find()) {
            return "GD" + gdAfterNdMatcher.group(1);
        }

        // Pattern 5: GD at start (but avoid "GD: +200,000")
        // Only match if NOT followed by + or number with comma
        Pattern gdStartPattern = Pattern.compile("^.*?GD[:\\s]+([0-9A-Z]{5,})(?![0-9,+])");
        Matcher gdStartMatcher = gdStartPattern.matcher(message);
        if (gdStartMatcher.find()) {
            String ref = gdStartMatcher.group(1);
            // Avoid matching amount like "200" from "GD: +200,000"
            if (!message.contains("GD: +" + ref) && !message.contains("GD:+" + ref)) {
                return "GD" + ref;
            }
        }

        // If no GD found, generate from timestamp
        log.warn("No transaction reference found, using timestamp");
        return "SMS" + System.currentTimeMillis();
    }

    /**
     * Extract username or deposit code from message content
     *
     * Patterns (in priority order):
     * 1. USER123 -> userId (handled by extractUserId)
     * 2. NAP TIEN DEMOACCOUNT -> username = DEMOACCOUNT
     * 3. NAPTIEN DEMOACCOUNT -> username = DEMOACCOUNT
     * 4. Just alphanumeric code: DEMOACCOUNT
     *
     * Format: Alphanumeric only (A-Z, 0-9), 3-30 characters
     * No special characters allowed (safe for bank transfer)
     *
     * IMPORTANT: Person transferring money MUST include their username/code
     * Example: "NAP TIEN DEMOACCOUNT" or "NAPTIEN VANA"
     */
    private String extractUsername(String message) {
        // Pattern 1: After "NAP TIEN " or "NAPTIEN "
        // Match alphanumeric word (3-30 chars) after NAP TIEN
        Pattern pattern1 = Pattern.compile("(?:NAP\\s*TIEN|NAPTIEN)\\s+([A-Z0-9]{3,30})");
        Matcher matcher1 = pattern1.matcher(message);
        if (matcher1.find()) {
            String username = matcher1.group(1);
            // Skip if it's USER{number} pattern (handled separately)
            if (!username.matches("USER\\d+")) {
                log.info("Extracted username after NAP TIEN: {}", username);
                return username;
            }
        }

        // Pattern 2: After "ND:" or "ND " (content field)
        // Look for alphanumeric word after ND field
        Pattern pattern2 = Pattern.compile("ND[:\\s]+([A-Z0-9]{3,30})");
        Matcher matcher2 = pattern2.matcher(message);
        if (matcher2.find()) {
            String username = matcher2.group(1);
            // Skip if it's USER{number} pattern (handled separately)
            if (!username.matches("USER\\d+")) {
                log.info("Extracted username after ND: {}", username);
                return username;
            }
        }

        // Pattern 3: Standalone alphanumeric code (fallback)
        // Find any alphanumeric sequence 4-30 chars that's not a number-only code
        Pattern pattern3 = Pattern.compile("\\b([A-Z]{2,}[A-Z0-9]{2,28})\\b");
        Matcher matcher3 = pattern3.matcher(message);
        if (matcher3.find()) {
            String username = matcher3.group(1);
            // Must have at least 2 letters (not just numbers)
            // Skip common bank codes and USER pattern
            if (!username.matches("USER\\d+") &&
                !username.matches("MBVCB.*") &&
                !username.matches("VND|GD|ND|SD|TK")) {
                log.info("Extracted username (standalone): {}", username);
                return username;
            }
        }

        log.warn("No username found in message: {}", message);
        return null;
    }

    /**
     * Extract email from message content
     *
     * Patterns:
     * - demo@gmail.com (anywhere in message)
     * - NAP TIEN demo@gmail.com
     * - ND: demo@gmail.com
     * - demo.account@gmail.com
     *
     * IMPORTANT: Person transferring money MUST include their email in transfer content
     * Example: "NAP TIEN demo@gmail.com" or just "demo@gmail.com"
     * @deprecated Email contains special characters not allowed in bank transfer notes
     */
    private String extractEmail(String message) {
        // Pattern: Standard email format (case-insensitive)
        // Matches: demo@gmail.com, user.name@example.com, etc.
        Pattern emailPattern = Pattern.compile("\\b([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,})\\b");
        Matcher emailMatcher = emailPattern.matcher(message);
        if (emailMatcher.find()) {
            String email = emailMatcher.group(1).toLowerCase(); // Convert to lowercase for consistency
            log.info("Extracted email: {}", email);
            return email;
        }

        log.warn("No email found in message: {}", message);
        log.warn("IMPORTANT: Transfer content MUST include email (e.g., 'demo@gmail.com' or 'NAP TIEN demo@gmail.com')");
        return null;
    }

    /**
     * Extract user ID from message content
     *
     * Patterns (search entire message, not just ND field):
     * - USER123 -> 123 (anywhere in message)
     * - USER 123 -> 123
     * - NAP TIEN USER123 -> 123
     * - ND: NAP TIEN USER123 -> 123
     * - ND:NAPTIEN USER123 -> 123
     * - NAPTIEN 123 -> 123
     * - U123 -> 123 (short form)
     *
     * IMPORTANT: Person transferring money MUST include USER{id} in transfer content
     * Example: "NAP TIEN USER123" or just "USER123"
     * @deprecated Use extractEmail instead for better user identification
     */
    private Long extractUserId(String message) {
        // Pattern 1: USER followed by number (anywhere in message)
        Pattern pattern1 = Pattern.compile("USER\\s*([0-9]+)");
        Matcher matcher1 = pattern1.matcher(message);
        if (matcher1.find()) {
            try {
                return Long.parseLong(matcher1.group(1));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse user ID from USER: {}", matcher1.group(1));
            }
        }

        // Pattern 2: U followed by number (short form - U123)
        Pattern pattern2 = Pattern.compile("\\bU([0-9]+)\\b");
        Matcher matcher2 = pattern2.matcher(message);
        if (matcher2.find()) {
            try {
                return Long.parseLong(matcher2.group(1));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse user ID from U: {}", matcher2.group(1));
            }
        }

        // Pattern 3: Just numbers after NAPTIEN or NAP TIEN
        Pattern pattern3 = Pattern.compile("NAP\\s*TIEN\\s+([0-9]+)");
        Matcher matcher3 = pattern3.matcher(message);
        if (matcher3.find()) {
            try {
                return Long.parseLong(matcher3.group(1));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse user ID from NAPTIEN: {}", matcher3.group(1));
            }
        }

        // Pattern 4: ND field with USER prefix (backward compatibility)
        Pattern pattern4 = Pattern.compile("ND[:\\s]*.*USER\\s*([0-9]+)");
        Matcher matcher4 = pattern4.matcher(message);
        if (matcher4.find()) {
            try {
                return Long.parseLong(matcher4.group(1));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse user ID from ND: {}", matcher4.group(1));
            }
        }

        log.warn("No user ID found in message: {}", message);
        log.warn("IMPORTANT: Transfer content MUST include USER{{id}} (e.g., 'USER123' or 'NAP TIEN USER123')");
        return null;
    }

    /**
     * Validate if SMS is from a known bank
     */
    public boolean isFromKnownBank(String sender) {
        if (sender == null) {
            return false;
        }

        String senderUpper = sender.toUpperCase();

        // Known bank SMS senders
        String[] knownBanks = {
            "VIETCOMBANK", "VCB",
            "TECHCOMBANK", "TCB",
            "BIDV",
            "AGRIBANK",
            "MBBANK", "MB",
            "VIETINBANK",
            "SACOMBANK",
            "ACB",
            "VPBANK",
            "TPBANK",
            "HDBANK",
            "SHBBANK", "SHB"
        };

        for (String bank : knownBanks) {
            if (senderUpper.contains(bank)) {
                return true;
            }
        }

        return false;
    }
}
