package com.aptech.aptechMall.service.admin;

import com.aptech.aptechMall.dto.admin.DashboardStatsResponse;
import com.aptech.aptechMall.entity.OrderStatus;
import com.aptech.aptechMall.repository.OrderRepository;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.repository.UserWalletRepository;
import com.aptech.aptechMall.security.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for admin dashboard statistics and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserWalletRepository walletRepository;

    /**
     * Get overall dashboard statistics
     * @return DashboardStatsResponse with aggregated stats
     */
    public DashboardStatsResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");

        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(Status.ACTIVE);

        // Order statistics
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);

        // Calculate total revenue (sum of all delivered orders)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatus(OrderStatus.DELIVERED);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        // Wallet statistics
        long totalWallets = walletRepository.count();
        long lockedWallets = walletRepository.countByIsLocked(true);

        // Calculate total wallet balance
        BigDecimal totalWalletBalance = walletRepository.sumAllBalances();
        if (totalWalletBalance == null) {
            totalWalletBalance = BigDecimal.ZERO;
        }

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue)
                .totalWallets(totalWallets)
                .lockedWallets(lockedWallets)
                .totalWalletBalance(totalWalletBalance)
                .build();
    }
}
