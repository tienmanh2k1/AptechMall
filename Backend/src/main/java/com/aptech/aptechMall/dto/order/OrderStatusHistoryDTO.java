package com.aptech.aptechMall.dto.order;

import com.aptech.aptechMall.entity.OrderStatus;
import com.aptech.aptechMall.entity.OrderStatusHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for order status history
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryDTO {

    private Long id;
    private OrderStatus status;
    private OrderStatus previousStatus;
    private String note;
    private Long changedBy; // Admin user ID or null for system
    private LocalDateTime createdAt;

    /**
     * Convert OrderStatusHistory entity to DTO
     * @param history OrderStatusHistory entity
     * @return OrderStatusHistoryDTO
     */
    public static OrderStatusHistoryDTO fromEntity(OrderStatusHistory history) {
        return OrderStatusHistoryDTO.builder()
                .id(history.getId())
                .status(history.getStatus())
                .previousStatus(history.getPreviousStatus())
                .note(history.getNote())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
