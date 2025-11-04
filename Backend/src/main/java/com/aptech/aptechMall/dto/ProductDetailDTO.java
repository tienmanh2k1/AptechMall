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
public class ProductDetailDTO {
    // Basic Info
    private String itemId;
    private String title;
    private String originalTitle;
    private String description;
    private String itemUrl;
    private String mainImage;
    private List<ImageDTO> images;
    private Boolean available;

    // Pricing
    private PriceDTO currentPrice;
    private PriceDTO promotionPrice;
    private Integer discountPercent;
    private Integer totalQuantity;

    // Category & Brand
    private String categoryId;
    private String brandName;
    private List<String> categoryPath;

    // Vendor/Store Info
    private VendorDTO vendor;

    // Variants/Options
    private List<VariantDTO> variants;

    // Reviews
    private ReviewSummaryDTO reviews;

    // Featured Values (sales, rating, etc.)
    private Integer totalSales;
    private Double rating;
    private Integer reviewCount;
    private Integer favoriteCount;

    // Physical specs
    private PhysicalParametersDTO physicalParameters;

    // Related products from same vendor
    private List<RelatedProductDTO> relatedProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceDTO {
        private Double originalPrice;
        private String currencyCode;
        private String currencySign;
        private String formattedPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDTO {
        private String url;
        private String smallUrl;
        private String mediumUrl;
        private String largeUrl;
        private Boolean isMain;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorDTO {
        private String vendorId;
        private String vendorName;
        private String displayName;
        private String shopUrl;
        private String logoUrl;
        private Integer positiveRating;
        private Integer totalFeedbacks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantDTO {
        private String variantId;
        private String name;
        private List<String> options;
        private String imageUrl;
        private Double price;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummaryDTO {
        private Integer totalReviews;
        private Double averageRating;
        private List<ReviewDTO> topReviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewDTO {
        private String reviewId;
        private String userName;
        private Integer rating;
        private String content;
        private String date;
        private String country;
        private List<String> images;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhysicalParametersDTO {
        private Double weight;
        private Integer length;
        private Integer width;
        private Integer height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedProductDTO {
        private String itemId;
        private String title;
        private String imageUrl;
        private String productUrl;
        private Double price;
        private String currencySign;
    }
}