package com.aptech.aptechMall.model.Aliexpress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Response model for the new OtAPI AliExpress API
 * Endpoint: /BatchGetItemFullInfo
 * Host: otapi-aliexpress.p.rapidapi.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliexpressProductDetailResponse {

    @JsonProperty("ErrorCode")
    private String errorCode;

    @JsonProperty("SubErrorCode")
    private Object subErrorCode;

    @JsonProperty("RequestId")
    private String requestId;

    @JsonProperty("RequestTime")
    private Double requestTime;

    @JsonProperty("Result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("Item")
        private Item item;

        @JsonProperty("Vendor")
        private Vendor vendor;

        @JsonProperty("RootPath")
        private RootPath rootPath;

        @JsonProperty("VendorItems")
        private VendorItems vendorItems;

        @JsonProperty("ProviderReviews")
        private ProviderReviews providerReviews;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("ErrorCode")
        private String errorCode;

        @JsonProperty("HasError")
        private Boolean hasError;

        @JsonProperty("ProviderType")
        private String providerType;

        @JsonProperty("Title")
        private String title;

        @JsonProperty("OriginalTitle")
        private String originalTitle;

        @JsonProperty("Description")
        private String description;

        @JsonProperty("CategoryId")
        private String categoryId;

        @JsonProperty("VendorId")
        private String vendorId;

        @JsonProperty("VendorName")
        private String vendorName;

        @JsonProperty("VendorDisplayName")
        private String vendorDisplayName;

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

        @JsonProperty("Price")
        private Price price;

        @JsonProperty("PromotionPrice")
        private Price promotionPrice;

        @JsonProperty("MasterQuantity")
        private Integer masterQuantity;

        @JsonProperty("Pictures")
        private List<Picture> pictures;

        @JsonProperty("Attributes")
        private List<Attribute> attributes;

        @JsonProperty("ConfiguredItems")
        private List<ConfiguredItem> configuredItems;

        @JsonProperty("Promotions")
        private List<Promotion> promotions;

        @JsonProperty("Features")
        private List<String> features;

        @JsonProperty("FeaturedValues")
        private List<FeaturedValue> featuredValues;

        @JsonProperty("PhysicalParameters")
        private PhysicalParameters physicalParameters;
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

        @JsonProperty("ConvertedPrice")
        private String convertedPrice;

        @JsonProperty("ConvertedPriceWithoutSign")
        private String convertedPriceWithoutSign;

        @JsonProperty("CurrencySign")
        private String currencySign;

        @JsonProperty("CurrencyName")
        private String currencyName;
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
    public static class Attribute {
        @JsonProperty("Pid")
        private String pid;

        @JsonProperty("Vid")
        private String vid;

        @JsonProperty("PropertyName")
        private String propertyName;

        @JsonProperty("Value")
        private String value;

        @JsonProperty("ValueAlias")
        private String valueAlias;

        @JsonProperty("OriginalPropertyName")
        private String originalPropertyName;

        @JsonProperty("OriginalValue")
        private String originalValue;

        @JsonProperty("IsConfigurator")
        private Boolean isConfigurator;

        @JsonProperty("ImageUrl")
        private String imageUrl;

        @JsonProperty("MiniImageUrl")
        private String miniImageUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfiguredItem {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("Quantity")
        private Integer quantity;

        @JsonProperty("SalesCount")
        private Integer salesCount;

        @JsonProperty("Configurators")
        private List<Configurator> configurators;

        @JsonProperty("Price")
        private Price price;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configurator {
        @JsonProperty("Pid")
        private String pid;

        @JsonProperty("Vid")
        private String vid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Promotion {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("Price")
        private Price price;

        @JsonProperty("ConfiguredItems")
        private List<ConfiguredItem> configuredItems;
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
        private Integer length;

        @JsonProperty("Width")
        private Integer width;

        @JsonProperty("Height")
        private Integer height;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Vendor {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("ProviderType")
        private String providerType;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("DisplayName")
        private String displayName;

        @JsonProperty("ShopName")
        private String shopName;

        @JsonProperty("Email")
        private String email;

        @JsonProperty("PictureUrl")
        private String pictureUrl;

        @JsonProperty("DisplayPictureUrl")
        private String displayPictureUrl;

        @JsonProperty("Credit")
        private VendorCredit credit;

        @JsonProperty("Features")
        private List<String> features;

        @JsonProperty("FeaturedValues")
        private List<FeaturedValue> featuredValues;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorCredit {
        @JsonProperty("Level")
        private Integer level;

        @JsonProperty("Score")
        private Integer score;

        @JsonProperty("TotalFeedbacks")
        private Integer totalFeedbacks;

        @JsonProperty("PositiveFeedbacks")
        private Integer positiveFeedbacks;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RootPath {
        @JsonProperty("Content")
        private List<Category> content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Category {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("ExternalId")
        private String externalId;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("ParentId")
        private String parentId;

        @JsonProperty("IsParent")
        private Boolean isParent;

        @JsonProperty("IsInternal")
        private Boolean isInternal;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorItems {
        @JsonProperty("Content")
        private List<VendorItem> content;

        @JsonProperty("TotalCount")
        private Integer totalCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorItem {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("Title")
        private String title;

        @JsonProperty("MainPictureUrl")
        private String mainPictureUrl;

        @JsonProperty("Price")
        private Price price;

        @JsonProperty("PromotionPrice")
        private Price promotionPrice;

        @JsonProperty("ExternalItemUrl")
        private String externalItemUrl;

        @JsonProperty("FeaturedValues")
        private List<FeaturedValue> featuredValues;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProviderReviews {
        @JsonProperty("Content")
        private List<Review> content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Review {
        @JsonProperty("ExternalId")
        private String externalId;

        @JsonProperty("ItemId")
        private String itemId;

        @JsonProperty("Content")
        private String content;

        @JsonProperty("CreatedDate")
        private String createdDate;

        @JsonProperty("UserNick")
        private String userNick;

        @JsonProperty("Rating")
        private Integer rating;

        @JsonProperty("Images")
        private List<String> images;

        @JsonProperty("FeaturedValues")
        private List<FeaturedValue> featuredValues;

        // Helper method to get country
        public String getCountry() {
            if (featuredValues != null) {
                return featuredValues.stream()
                        .filter(fv -> "country".equals(fv.getName()))
                        .map(FeaturedValue::getValue)
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }

        // Helper method to get language
        public String getLanguage() {
            if (featuredValues != null) {
                return featuredValues.stream()
                        .filter(fv -> "language".equals(fv.getName()))
                        .map(FeaturedValue::getValue)
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }
    }
}
