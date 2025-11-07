package com.aptech.aptechMall.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating order shipping address
 * User can update when status is PENDING
 * Admin/Staff can update when status is PENDING or CONFIRMED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderAddressRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[\\d\\s\\-+()]{8,}$", message = "Please enter a valid phone number")
    private String phone;

    /**
     * Optional note explaining the address change
     */
    private String note;
}
