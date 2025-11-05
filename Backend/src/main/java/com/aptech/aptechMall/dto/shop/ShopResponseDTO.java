package com.aptech.aptechMall.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String shopUrl;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

