package com.aptech.aptechMall.dto.cart;

import com.aptech.aptechMall.entity.Marketplace;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for adding a product to cart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 500, message = "Product name must not exceed 500 characters")
    private String productName;

    @Size(max = 1000, message = "Product image URL must not exceed 1000 characters")
    private String productImage;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 999, message = "Quantity must not exceed 999")
    private Integer quantity;

    @NotNull(message = "Marketplace is required")
    private Marketplace marketplace;

    // Variant information (optional - for products with variants)
    private String variantId;

    @Size(max = 200, message = "Variant name must not exceed 200 characters")
    private String variantName;

    @Size(max = 500, message = "Variant options must not exceed 500 characters")
    private String variantOptions; // JSON string of selected options, e.g., "Color: Red, Size: M"
}
