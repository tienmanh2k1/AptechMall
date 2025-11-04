package com.aptech.aptechMall.model.Aliexpress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Model for new AliExpress DataHub API search response
 * Structure: Root -> Result -> Items -> Items -> Content (array)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliexpressProductSearchResponse {

    @JsonProperty("ErrorCode")
    private String errorCode;

    @JsonProperty("SubErrorCode")
    private Map<String, Object> subErrorCode;

    @JsonProperty("RequestId")
    private String requestId;

    @JsonProperty("RequestTime")
    private Double requestTime;

    @JsonProperty("Result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("Items")
        private ItemsWrapper items;

        @JsonProperty("SearchProperties")
        private Map<String, Object> searchProperties;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemsWrapper {
        @JsonProperty("Items")
        private ItemsData items;

        @JsonProperty("Categories")
        private Map<String, Object> categories;

        @JsonProperty("Provider")
        private String provider;

        @JsonProperty("SearchMethod")
        private String searchMethod;

        @JsonProperty("CurrentSort")
        private String currentSort;

        @JsonProperty("CurrentFrameSize")
        private Integer currentFrameSize;

        @JsonProperty("MaximumPageCount")
        private Integer maximumPageCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemsData {
        @JsonProperty("Content")
        private List<Product> content;

        @JsonProperty("TotalCount")
        private Integer totalCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("ErrorCode")
        private String errorCode;

        @JsonProperty("HasError")
        private Boolean hasError;

        @JsonProperty("ProviderType")
        private String providerType;

        @JsonProperty("UpdatedTime")
        private String updatedTime;

        @JsonProperty("CreatedTime")
        private String createdTime;

        @JsonProperty("Title")
        private String title;

        @JsonProperty("OriginalTitle")
        private String originalTitle;

        @JsonProperty("CategoryId")
        private String categoryId;

        @JsonProperty("ExternalCategoryId")
        private String externalCategoryId;

        @JsonProperty("VendorId")
        private String vendorId;

        @JsonProperty("VendorName")
        private String vendorName;

        @JsonProperty("VendorDisplayName")
        private String vendorDisplayName;

        @JsonProperty("VendorScore")
        private Integer vendorScore;

        @JsonProperty("BrandId")
        private String brandId;

        @JsonProperty("BrandName")
        private String brandName;

        @JsonProperty("alternativeUrl")
        private String alternativeUrl;

        @JsonProperty("ExternalItemUrl")
        private String externalItemUrl;

        @JsonProperty("MainPictureUrl")
        private String mainPictureUrl;

        @JsonProperty("StuffStatus")
        private String stuffStatus;

        @JsonProperty("Volume")
        private Integer volume;

        @JsonProperty("Price")
        private Price price;

        @JsonProperty("MasterQuantity")
        private Integer masterQuantity;

        @JsonProperty("Pictures")
        private List<Picture> pictures;

        @JsonProperty("Location")
        private Map<String, Object> location;

        @JsonProperty("Features")
        private List<String> features;

        @JsonProperty("FeaturedValues")
        private List<FeaturedValue> featuredValues;

        @JsonProperty("IsSellAllowed")
        private Boolean isSellAllowed;

        @JsonProperty("PhysicalParameters")
        private PhysicalParameters physicalParameters;

        @JsonProperty("IsFiltered")
        private Boolean isFiltered;

        @JsonProperty("PromotionPrice")
        private PromotionPrice promotionPrice;

        @JsonProperty("PromotionPricePercent")
        private List<PromotionPricePercent> promotionPricePercent;

        // Helper methods
        public String getItemIdNumeric() {
            if (id == null) return null;
            // Extract numeric part from "ae-1005005891237816"
            return id.replace("ae-", "");
        }

        public Integer getRating() {
            if (featuredValues == null) return 0;
            String ratingStr = featuredValues.stream()
                    .filter(fv -> "rating".equals(fv.getName()))
                    .findFirst()
                    .map(FeaturedValue::getValue)
                    .orElse("0");

            try {
                // Parse decimal values (e.g., "4.5", "4.7") and round to nearest integer
                return (int) Math.round(Double.parseDouble(ratingStr));
            } catch (NumberFormatException e) {
                return 0; // Fallback to 0 if parsing fails
            }
        }

        public Integer getReviewCount() {
            if (featuredValues == null) return 0;
            String reviewStr = featuredValues.stream()
                    .filter(fv -> "reviews".equals(fv.getName()))
                    .findFirst()
                    .map(FeaturedValue::getValue)
                    .orElse("0");

            try {
                // Handle special formats: "1.2k", "5k+", "10K", etc.
                reviewStr = reviewStr.toLowerCase().replace("+", "").trim();

                if (reviewStr.endsWith("k")) {
                    // Convert "1.2k" -> 1200, "5k" -> 5000
                    double value = Double.parseDouble(reviewStr.replace("k", ""));
                    return (int) (value * 1000);
                } else if (reviewStr.endsWith("m")) {
                    // Handle millions if needed: "1.5m" -> 1500000
                    double value = Double.parseDouble(reviewStr.replace("m", ""));
                    return (int) (value * 1000000);
                }

                // Normal integer parsing
                return Integer.parseInt(reviewStr);
            } catch (NumberFormatException e) {
                return 0; // Fallback to 0 if parsing fails
            }
        }

        public String getTotalSales() {
            if (featuredValues == null) return null;
            return featuredValues.stream()
                    .filter(fv -> "TotalSales".equals(fv.getName()))
                    .findFirst()
                    .map(FeaturedValue::getValue)
                    .orElse(null);
        }

        public boolean hasPromotion() {
            return promotionPrice != null &&
                   promotionPrice.getOriginalPrice() != null &&
                   promotionPrice.getOriginalPrice() > 0;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        @JsonProperty("OriginalPrice")
        private Double originalPrice;

        @JsonProperty("MarginPrice")
        private Double marginPrice;

        @JsonProperty("OriginalCurrencyCode")
        private String originalCurrencyCode;

        @JsonProperty("ConvertedPriceList")
        private Map<String, Object> convertedPriceList;

        @JsonProperty("ConvertedPrice")
        private String convertedPrice;

        @JsonProperty("ConvertedPriceWithoutSign")
        private String convertedPriceWithoutSign;

        @JsonProperty("CurrencySign")
        private String currencySign;

        @JsonProperty("CurrencyName")
        private String currencyName;

        @JsonProperty("IsDeliverable")
        private Boolean isDeliverable;

        @JsonProperty("DeliveryPrice")
        private Map<String, Object> deliveryPrice;

        @JsonProperty("OneItemDeliveryPrice")
        private Map<String, Object> oneItemDeliveryPrice;

        @JsonProperty("PriceWithoutDelivery")
        private Map<String, Object> priceWithoutDelivery;

        @JsonProperty("OneItemPriceWithoutDelivery")
        private Map<String, Object> oneItemPriceWithoutDelivery;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Picture {
        @JsonProperty("Url")
        private String url;

        @JsonProperty("Small")
        private ImageSize small;

        @JsonProperty("Medium")
        private ImageSize medium;

        @JsonProperty("Large")
        private ImageSize large;

        @JsonProperty("IsMain")
        private Boolean isMain;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageSize {
        @JsonProperty("Url")
        private String url;

        @JsonProperty("Width")
        private Integer width;

        @JsonProperty("Height")
        private Integer height;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeaturedValue {
        @JsonProperty("Name")
        private String name;

        @JsonProperty("Value")
        private String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhysicalParameters {
        @JsonProperty("Weight")
        private Double weight;

        @JsonProperty("Length")
        private Double length;

        @JsonProperty("Width")
        private Double width;

        @JsonProperty("Height")
        private Double height;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromotionPrice {
        @JsonProperty("OriginalPrice")
        private Double originalPrice;

        @JsonProperty("MarginPrice")
        private Double marginPrice;

        @JsonProperty("OriginalCurrencyCode")
        private String originalCurrencyCode;

        @JsonProperty("ConvertedPriceList")
        private Map<String, Object> convertedPriceList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromotionPricePercent {
        @JsonProperty("CurrencyCode")
        private String currencyCode;

        @JsonProperty("Percent")
        private Integer percent;
    }
}
