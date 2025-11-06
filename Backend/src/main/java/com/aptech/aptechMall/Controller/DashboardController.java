package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.dto.admin.DashboardStatsResponse;
import com.aptech.aptechMall.service.admin.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for admin dashboard
 * All endpoints require ADMIN role
 * Base path: /api/admin/dashboard
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class DashboardController {


    private final DashboardService dashboardService;

    /**
     * Get overall dashboard statistics
     * GET /api/admin/dashboard/stats
     * @return DashboardStatsResponse with aggregated statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        try {
            log.info("GET /api/admin/dashboard/stats");

            DashboardStatsResponse stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard statistics retrieved successfully"));

        } catch (Exception e) {
            log.error("Error getting dashboard stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "Failed to get dashboard statistics", e.getMessage()));
        }
    }
}
