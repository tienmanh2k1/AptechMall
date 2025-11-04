package com.aptech.aptechMall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDTO {
    private SearchMeta meta;
    private List<ProductSummaryDTO> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMeta {
        private String keyword;
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalResults;
        private List<String> sortOptions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummaryDTO {
        private String itemId;           // Changed to String to support "ae-xxx" format
        private String itemIdNumeric;     // Numeric part only
        private String title;
        private String imageUrl;
        private String productUrl;
        private String currentPrice;
        private String originalPrice;
        private String currencySign;
        private Integer salesCount;
        private Boolean hasDiscount;

        // New fields from new API
        private Integer rating;            // Product rating (e.g., "4.5")
        private Integer reviewCount;       // Number of reviews
        private String vendorName;        // Store/vendor name
        private String brandName;         // Brand name
        private Integer promotionPercent; // Discount percentage
        private List<String> imageUrls;   // Multiple image URLs
    }
}