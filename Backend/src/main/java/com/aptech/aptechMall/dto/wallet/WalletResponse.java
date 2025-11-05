package com.aptech.aptechMall.dto.wallet;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private boolean isLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
