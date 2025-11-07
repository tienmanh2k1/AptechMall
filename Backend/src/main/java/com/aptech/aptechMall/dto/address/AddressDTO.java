package com.aptech.aptechMall.dto.address;

import com.aptech.aptechMall.model.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for UserAddress response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private Long id;
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private AddressType addressType;
    private boolean isDefault;
    private String fullAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
