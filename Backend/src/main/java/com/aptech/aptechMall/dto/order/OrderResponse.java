package com.aptech.aptechMall.dto.order;

import com.aptech.aptechMall.entity.Order;
import com.aptech.aptechMall.entity.OrderPaymentStatus;
import com.aptech.aptechMall.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO representing an order in response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String phone;
    private String note;
    private Integer totalItems;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Payment information
    private OrderPaymentStatus paymentStatus;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private BigDecimal productCost;
    private BigDecimal serviceFee;
    private BigDecimal domesticShippingFee;
    private BigDecimal internationalShippingFee;
    private BigDecimal additionalServicesFee; // Phí dịch vụ: đóng gỗ, bọt khí, kiểm đếm
    private BigDecimal estimatedWeight; // Cân nặng ước tính (kg)
    private BigDecimal vietnamDomesticShippingFee;
    private Boolean isCodShipping;

    // Status history
    private List<OrderStatusHistoryDTO> statusHistory;

    /**
     * Convert Order entity to DTO
     * @param order Order entity
     * @return OrderResponse
     */
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(OrderItemDTO::fromEntity)
                .collect(Collectors.toList());

        List<OrderStatusHistoryDTO> historyDTOs = order.getStatusHistory() != null
                ? order.getStatusHistory().stream()
                        .map(OrderStatusHistoryDTO::fromEntity)
                        .collect(Collectors.toList())
                : new ArrayList<>();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .phone(order.getPhone())
                .note(order.getNote())
                .totalItems(order.getTotalItems())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                // Payment information
                .paymentStatus(order.getPaymentStatus())
                .depositAmount(order.getDepositAmount())
                .remainingAmount(order.getRemainingAmount())
                .productCost(order.getProductCost())
                .serviceFee(order.getServiceFee())
                .domesticShippingFee(order.getDomesticShippingFee())
                .internationalShippingFee(order.getInternationalShippingFee())
                .additionalServicesFee(order.getAdditionalServicesFee())
                .estimatedWeight(order.getEstimatedWeight())
                .vietnamDomesticShippingFee(order.getVietnamDomesticShippingFee())
                .isCodShipping(order.isCodShipping())
                // Status history
                .statusHistory(historyDTOs)
                .build();
    }

    /**
     * Convert Order entity to summary DTO (without items)
     * @param order Order entity
     * @return OrderResponse
     */
    public static OrderResponse toSummary(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .phone(order.getPhone())
                .note(order.getNote())
                .totalItems(order.getTotalItems())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                // Include payment status for summary
                .paymentStatus(order.getPaymentStatus())
                .depositAmount(order.getDepositAmount())
                .remainingAmount(order.getRemainingAmount())
                .build();
    }
}
