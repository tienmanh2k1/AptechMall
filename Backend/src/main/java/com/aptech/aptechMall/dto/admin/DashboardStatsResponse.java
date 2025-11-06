package com.aptech.aptechMall.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for admin dashboard statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsResponse {
    /**
     * Total number of users in the system
     */
    private Long totalUsers;

    /**
     * Total number of orders
     */
    private Long totalOrders;

    /**
     * Total revenue from all orders
     */
    private BigDecimal totalRevenue;

    /**
     * Total number of wallets
     */
    private Long totalWallets;

    /**
     * Total balance across all wallets
     */
    private BigDecimal totalWalletBalance;

    /**
     * Number of pending orders
     */
    private Long pendingOrders;

    /**
     * Number of active users (not suspended/deleted)
     */
    private Long activeUsers;

    /**
     * Number of locked wallets
     */
    private Long lockedWallets;
}
