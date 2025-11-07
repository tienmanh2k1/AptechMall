package com.aptech.aptechMall.dto.wallet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for user wallet information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletResponse {
    private Long walletId;
    private Long userId;
    private BigDecimal balance;

    @JsonProperty("isLocked")
    private boolean isLocked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * User information (for admin view)
     */
    private String username;
    private String email;
    private String fullName;

    /**
     * Deposit code for bank transfer
     * Format: Username (if exists) or USER{userId}
     * No special characters - safe for bank transfer notes
     */
    private String depositCode;
}
