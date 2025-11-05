package com.aptech.aptechMall.dto.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopRequestDTO {
    @NotBlank(message = "Shop name is required")
    @Size(max = 200, message = "Shop name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 500, message = "Shop URL must not exceed 500 characters")
    private String shopUrl;

    @Size(max = 100, message = "Contact email must not exceed 100 characters")
    private String contactEmail;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    private String contactPhone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Boolean isActive;
}

