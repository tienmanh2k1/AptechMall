package com.aptech.aptechMall.service;

import com.aptech.aptechMall.dto.ProductDetailDTO;
import com.aptech.aptechMall.dto.ProductSearchDTO;
import com.aptech.aptechMall.model.Aliexpress.AliexpressProductSearchResponse;
import com.aptech.aptechMall.model.Aliexpress.AliexpressProductDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class AliExpressService implements ProductMarketplaceService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${rapidapi.aliexpress.key}")
    private String apiKey;

    @Value("${rapidapi.aliexpress.host}")
    private String apiHost;

    public AliExpressService(
            @Value("${rapidapi.aliexpress.base-url}") String baseUrl,
            ObjectMapper objectMapper) {

        // Increase buffer size to 10MB to handle large API responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = objectMapper;

        log.info("WebClient initialized with 10MB buffer size for large API responses");
    }

    /**
     * Get product details by ID with detailed logging (full response) - NEW OtAPI FORMAT
     * Uses: https://otapi-aliexpress.p.rapidapi.com/BatchGetItemFullInfo
     */
    public Mono<AliexpressProductDetailResponse> getProductDetailsFull(String productId) {
        log.info("Getting product details (OtAPI) for ID: {}", productId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/BatchGetItemFullInfo")
                        .queryParam("language", "en")
                        .queryParam("itemId", productId)
                        .build())
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", apiHost)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Error response from OtAPI: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Error body: {}", errorBody);
                                return Mono.error(new RuntimeException("OtAPI Error: " + errorBody));
                            });
                })
                .bodyToMono(String.class)  // Get as String first for debugging
                .doOnNext(jsonString -> {
                    log.debug("Raw JSON response (OtAPI) for product {}: {}", productId,
                            jsonString.substring(0, Math.min(500, jsonString.length())) + "...");
                })
                .flatMap(jsonString -> {
                    try {
                        // Parse JSON string to new OtAPI model
                        AliexpressProductDetailResponse response = objectMapper.readValue(
                                jsonString, AliexpressProductDetailResponse.class);

                        // Check for API error
                        if (!"Ok".equalsIgnoreCase(response.getErrorCode())) {
                            log.error("OtAPI returned error: {}", response.getErrorCode());
                            return Mono.error(new RuntimeException("OtAPI Error: " + response.getErrorCode()));
                        }

                        log.info("Successfully parsed product details (OtAPI) for ID: {}", productId);
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Error parsing OtAPI JSON for product {}: {}", productId, e.getMessage());
                        log.error("Problematic JSON: {}", jsonString);
                        return Mono.error(new RuntimeException("JSON parsing error: " + e.getMessage(), e));
                    }
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(WebClientResponseException.class, error ->
                        log.error("WebClient error getting product details (OtAPI): Status={}, Body={}",
                                error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(error ->
                        log.error("Error getting product details (OtAPI) for ID {}: {}", productId, error.getMessage(), error));
    }





    /**
     * Search products and return simplified DTO
     * Implementation of ProductMarketplaceService interface
     * UPDATED: Now uses BatchSearchItemsFrame endpoint
     */
    @Override
    public Mono<ProductSearchDTO> searchProducts(String keyword, int page, int sort) {
        log.info("searchProducts called - redirecting to BatchSearchItemsFrame");

        // Convert page to framePosition
        int frameSize = 10;
        int framePosition = (page - 1) * frameSize;

        // Redirect to new API implementation (sort parameter is ignored)
        return searchProductsSimplified(keyword, "en", framePosition, frameSize);
    }

    /**
     * Get product reviews
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public Mono<String> getProductReviews(String productId, int page) {
        log.info("Getting reviews for product: {}, page: {}", productId, page);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/product/reviews")
                        .queryParam("productId", productId)
                        .queryParam("page", page)
                        .build())
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", apiHost)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .doOnError(error ->
                        log.error("Error getting reviews for product {}: {}", productId, error.getMessage()));
    }

    /**
     * Get product details as simplified DTO - NEW OtAPI FORMAT
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public Mono<ProductDetailDTO> getProductDetails(String productId) {
        log.info("Getting simplified product details (OtAPI) for ID: {}", productId);

        return getProductDetailsFull(productId)
                .map(response -> {
                    try {
                        if (response == null || response.getResult() == null) {
                            log.error("Response or result is null for product ID: {}", productId);
                            throw new RuntimeException("Invalid response from OtAPI");
                        }

                        var item = response.getResult().getItem();
                        if (item == null) {
                            log.error("Item is null for product ID: {}", productId);
                            throw new RuntimeException("Product item not found");
                        }

                        var vendor = response.getResult().getVendor();
                        var rootPath = response.getResult().getRootPath();
                        var vendorItems = response.getResult().getVendorItems();
                        var providerReviews = response.getResult().getProviderReviews();

                        // === BASIC INFO ===
                        String itemId = item.getId();
                        String title = item.getTitle();
                        String originalTitle = item.getOriginalTitle();
                        String description = item.getDescription();
                        String itemUrl = item.getExternalItemUrl();
                        String mainImage = item.getMainPictureUrl();

                        // === IMAGES ===
                        List<ProductDetailDTO.ImageDTO> images = List.of();
                        if (item.getPictures() != null) {
                            images = item.getPictures().stream()
                                    .map(pic -> ProductDetailDTO.ImageDTO.builder()
                                            .url(pic.getUrl())
                                            .smallUrl(pic.getSmall() != null ? pic.getSmall().getUrl() : null)
                                            .mediumUrl(pic.getMedium() != null ? pic.getMedium().getUrl() : null)
                                            .largeUrl(pic.getLarge() != null ? pic.getLarge().getUrl() : null)
                                            .isMain(pic.getIsMain())
                                            .build())
                                    .toList();
                        }

                        // === PRICING ===
                        ProductDetailDTO.PriceDTO currentPrice = null;
                        ProductDetailDTO.PriceDTO promotionPrice = null;
                        Integer discountPercent = null;

                        if (item.getPrice() != null) {
                            currentPrice = ProductDetailDTO.PriceDTO.builder()
                                    .originalPrice(item.getPrice().getOriginalPrice())
                                    .currencyCode(item.getPrice().getOriginalCurrencyCode())
                                    .currencySign(item.getPrice().getCurrencySign())
                                    .formattedPrice(item.getPrice().getConvertedPrice())
                                    .build();
                        }

                        if (item.getPromotionPrice() != null) {
                            promotionPrice = ProductDetailDTO.PriceDTO.builder()
                                    .originalPrice(item.getPromotionPrice().getOriginalPrice())
                                    .currencyCode(item.getPromotionPrice().getOriginalCurrencyCode())
                                    .currencySign(item.getPromotionPrice().getCurrencySign())
                                    .formattedPrice(item.getPromotionPrice().getConvertedPrice())
                                    .build();

                            // Calculate discount percentage
                            if (currentPrice != null && currentPrice.getOriginalPrice() != null
                                    && promotionPrice.getOriginalPrice() != null) {
                                double original = currentPrice.getOriginalPrice();
                                double promo = promotionPrice.getOriginalPrice();
                                if (original > 0) {
                                    discountPercent = (int) Math.round(((original - promo) / original) * 100);
                                }
                            }
                        }

                        // === CATEGORY PATH ===
                        List<String> categoryPath = List.of();
                        if (rootPath != null && rootPath.getContent() != null) {
                            categoryPath = rootPath.getContent().stream()
                                    .map(AliexpressProductDetailResponse.Category::getName)
                                    .filter(name -> name != null && !name.isEmpty())
                                    .toList();
                        }

                        // === VENDOR INFO ===
                        ProductDetailDTO.VendorDTO vendorDTO = null;
                        if (vendor != null) {
                            Integer positiveRating = null;
                            if (vendor.getCredit() != null && vendor.getCredit().getTotalFeedbacks() != null
                                    && vendor.getCredit().getTotalFeedbacks() > 0) {
                                positiveRating = (int) Math.round(
                                    (vendor.getCredit().getPositiveFeedbacks() * 100.0) / vendor.getCredit().getTotalFeedbacks()
                                );
                            }

                            vendorDTO = ProductDetailDTO.VendorDTO.builder()
                                    .vendorId(vendor.getId())
                                    .vendorName(vendor.getName())
                                    .displayName(vendor.getDisplayName())
                                    .shopUrl(getShopUrl(vendor.getFeaturedValues()))
                                    .logoUrl(vendor.getDisplayPictureUrl())
                                    .positiveRating(positiveRating)
                                    .totalFeedbacks(vendor.getCredit() != null ? vendor.getCredit().getTotalFeedbacks() : null)
                                    .build();
                        }

                        // === VARIANTS (from ConfiguredItems) ===
                        List<ProductDetailDTO.VariantDTO> variants = List.of();
                        if (item.getConfiguredItems() != null && !item.getConfiguredItems().isEmpty()) {
                            variants = item.getConfiguredItems().stream()
                                    .limit(10) // Limit to top 10 variants
                                    .map(config -> {
                                        String variantName = buildVariantName(config.getConfigurators(), item.getAttributes());
                                        return ProductDetailDTO.VariantDTO.builder()
                                                .variantId(config.getId())
                                                .name(variantName)
                                                .options(List.of()) // Can be expanded if needed
                                                .price(config.getPrice() != null ? config.getPrice().getOriginalPrice() : null)
                                                .quantity(config.getQuantity())
                                                .build();
                                    })
                                    .toList();
                        }

                        // === REVIEWS ===
                        ProductDetailDTO.ReviewSummaryDTO reviewSummary = null;
                        Double averageRating = null;
                        Integer totalReviews = 0;
                        Integer reviewCount = 0;

                        // Extract rating and review count from FeaturedValues
                        if (item.getFeaturedValues() != null) {
                            for (var fv : item.getFeaturedValues()) {
                                if ("rating".equals(fv.getName())) {
                                    try {
                                        averageRating = Double.parseDouble(fv.getValue());
                                    } catch (NumberFormatException e) {
                                        log.warn("Could not parse rating: {}", fv.getValue());
                                    }
                                } else if ("reviews".equals(fv.getName())) {
                                    try {
                                        reviewCount = Integer.parseInt(fv.getValue());
                                    } catch (NumberFormatException e) {
                                        log.warn("Could not parse review count: {}", fv.getValue());
                                    }
                                } else if ("TotalSales".equals(fv.getName())) {
                                    // Handled below
                                }
                            }
                        }

                        List<ProductDetailDTO.ReviewDTO> topReviews = List.of();
                        if (providerReviews != null && providerReviews.getContent() != null) {
                            totalReviews = providerReviews.getContent().size();
                            topReviews = providerReviews.getContent().stream()
                                    .limit(5) // Top 5 reviews
                                    .map(review -> ProductDetailDTO.ReviewDTO.builder()
                                            .reviewId(review.getExternalId())
                                            .userName(review.getUserNick())
                                            .rating(review.getRating())
                                            .content(review.getContent())
                                            .date(review.getCreatedDate())
                                            .country(review.getCountry())
                                            .images(review.getImages())
                                            .build())
                                    .toList();
                        }

                        if (totalReviews > 0 || reviewCount > 0) {
                            reviewSummary = ProductDetailDTO.ReviewSummaryDTO.builder()
                                    .totalReviews(Math.max(totalReviews, reviewCount))
                                    .averageRating(averageRating)
                                    .topReviews(topReviews)
                                    .build();
                        }

                        // === FEATURED VALUES ===
                        Integer totalSales = null;
                        Integer favoriteCount = null;

                        if (item.getFeaturedValues() != null) {
                            for (var fv : item.getFeaturedValues()) {
                                if ("TotalSales".equals(fv.getName())) {
                                    try {
                                        totalSales = Integer.parseInt(fv.getValue());
                                    } catch (NumberFormatException e) {
                                        log.warn("Could not parse total sales: {}", fv.getValue());
                                    }
                                } else if ("favCount".equals(fv.getName())) {
                                    try {
                                        favoriteCount = Integer.parseInt(fv.getValue());
                                    } catch (NumberFormatException e) {
                                        log.warn("Could not parse favorite count: {}", fv.getValue());
                                    }
                                }
                            }
                        }

                        // === PHYSICAL PARAMETERS ===
                        ProductDetailDTO.PhysicalParametersDTO physicalParams = null;
                        if (item.getPhysicalParameters() != null) {
                            physicalParams = ProductDetailDTO.PhysicalParametersDTO.builder()
                                    .weight(item.getPhysicalParameters().getWeight())
                                    .length(item.getPhysicalParameters().getLength())
                                    .width(item.getPhysicalParameters().getWidth())
                                    .height(item.getPhysicalParameters().getHeight())
                                    .build();
                        }

                        // === RELATED PRODUCTS ===
                        List<ProductDetailDTO.RelatedProductDTO> relatedProducts = List.of();
                        if (vendorItems != null && vendorItems.getContent() != null) {
                            relatedProducts = vendorItems.getContent().stream()
                                    .limit(10) // Top 10 related products
                                    .map(relatedItem -> ProductDetailDTO.RelatedProductDTO.builder()
                                            .itemId(relatedItem.getId())
                                            .title(relatedItem.getTitle())
                                            .imageUrl(relatedItem.getMainPictureUrl())
                                            .productUrl(relatedItem.getExternalItemUrl())
                                            .price(relatedItem.getPrice() != null ?
                                                    relatedItem.getPrice().getOriginalPrice() : null)
                                            .currencySign(relatedItem.getPrice() != null ?
                                                    relatedItem.getPrice().getCurrencySign() : "$")
                                            .build())
                                    .toList();
                        }

                        // === BUILD FINAL DTO ===
                        return ProductDetailDTO.builder()
                                .itemId(itemId)
                                .title(title)
                                .originalTitle(originalTitle)
                                .description(description)
                                .itemUrl(itemUrl)
                                .mainImage(mainImage)
                                .images(images)
                                .available(true) // OtAPI doesn't have this field explicitly
                                .currentPrice(currentPrice)
                                .promotionPrice(promotionPrice)
                                .discountPercent(discountPercent)
                                .totalQuantity(item.getMasterQuantity())
                                .categoryId(item.getCategoryId())
                                .brandName(item.getBrandName())
                                .categoryPath(categoryPath)
                                .vendor(vendorDTO)
                                .variants(variants)
                                .reviews(reviewSummary)
                                .totalSales(totalSales)
                                .rating(averageRating)
                                .reviewCount(reviewCount)
                                .favoriteCount(favoriteCount)
                                .physicalParameters(physicalParams)
                                .relatedProducts(relatedProducts)
                                .build();

                    } catch (Exception e) {
                        log.error("Error mapping OtAPI product details to DTO: {}", e.getMessage(), e);
                        throw new RuntimeException("Error processing product details", e);
                    }
                })
                .doOnError(error ->
                        log.error("Error getting simplified product details (OtAPI) for ID {}: {}",
                                productId, error.getMessage()));
    }

    /**
     * Helper method to extract shop URL from vendor's featured values
     */
    private String getShopUrl(List<AliexpressProductDetailResponse.FeaturedValue> featuredValues) {
        if (featuredValues != null) {
            return featuredValues.stream()
                    .filter(fv -> "shopUrl".equals(fv.getName()))
                    .map(AliexpressProductDetailResponse.FeaturedValue::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Helper method to build variant name from configurators
     */
    private String buildVariantName(List<AliexpressProductDetailResponse.Configurator> configurators,
                                      List<AliexpressProductDetailResponse.Attribute> attributes) {
        if (configurators == null || configurators.isEmpty() || attributes == null) {
            return "Default";
        }

        return configurators.stream()
                .map(config -> {
                    return attributes.stream()
                            .filter(attr -> config.getPid().equals(attr.getPid())
                                    && config.getVid().equals(attr.getVid()))
                            .map(attr -> attr.getPropertyName() + ": " + attr.getValue())
                            .findFirst()
                            .orElse("Unknown");
                })
                .filter(name -> !"Unknown".equals(name))
                .reduce((a, b) -> a + ", " + b)
                .orElse("Default");
    }

    /**
     * Search products using BatchSearchItemsFrame endpoint (full response)
     * Endpoint: /BatchSearchItemsFrame
     */
    public Mono<AliexpressProductSearchResponse> searchProductsNewAPI(String keyword, String language,
                                                                      int framePosition, int frameSize) {
        log.info("Searching products with BatchSearchItemsFrame - keyword: {}, language: {}, framePosition: {}, frameSize: {}",
                 keyword, language, framePosition, frameSize);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/BatchSearchItemsFrame")
                        .queryParam("ItemTitle", keyword)
                        .queryParam("language", language)
                        .queryParam("framePosition", framePosition)
                        .queryParam("frameSize", frameSize)
                        .build())
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", apiHost)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Error response from BatchSearchItemsFrame API: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Error body: {}", errorBody);
                                return Mono.error(new RuntimeException("BatchSearchItemsFrame API Error: " + errorBody));
                            });
                })
                .bodyToMono(String.class)
                .doOnNext(jsonString -> {
                    log.debug("Raw JSON response (BatchSearchItemsFrame): {}",
                            jsonString.substring(0, Math.min(1000, jsonString.length())) + "...");
                })
                .flatMap(jsonString -> {
                    try {
                        AliexpressProductSearchResponse response = objectMapper.readValue(
                                jsonString, AliexpressProductSearchResponse.class);
                        log.info("Successfully parsed BatchSearchItemsFrame results - ErrorCode: {}",
                                response.getErrorCode());
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Error parsing BatchSearchItemsFrame JSON: {}", e.getMessage());
                        log.error("Problematic JSON: {}", jsonString);
                        return Mono.error(new RuntimeException("JSON parsing error: " + e.getMessage(), e));
                    }
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(WebClientResponseException.class, error ->
                        log.error("WebClient error (BatchSearchItemsFrame): Status={}, Body={}",
                                error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(error ->
                        log.error("Error searching products (BatchSearchItemsFrame): {}", error.getMessage(), error));
    }

    /**
     * Search products using BatchSearchItemsFrame and return simplified DTO
     */
    public Mono<ProductSearchDTO> searchProductsSimplified(String keyword, String language,
                                                            int framePosition, int frameSize) {
        return searchProductsNewAPI(keyword, language, framePosition, frameSize)
                .map(response -> {
                    if (response == null ||
                        response.getResult() == null ||
                        response.getResult().getItems() == null ||
                        response.getResult().getItems().getItems() == null) {
                        log.warn("Invalid response structure from NEW API");
                        return ProductSearchDTO.builder()
                                .products(List.of())
                                .build();
                    }

                    var itemsData = response.getResult().getItems().getItems();
                    var itemsWrapper = response.getResult().getItems();

                    // Build meta
                    // Calculate current page from framePosition (framePosition / frameSize)
                    int currentPage = frameSize > 0 ? (framePosition / frameSize) + 1 : 1;

                    ProductSearchDTO.SearchMeta meta = ProductSearchDTO.SearchMeta.builder()
                            .keyword(keyword)
                            .currentPage(currentPage)
                            .pageSize(frameSize)
                            .totalResults(itemsData.getTotalCount() != null ?
                                         itemsData.getTotalCount() : 0)
                            .sortOptions(List.of("Default", "PriceAsc", "PriceDesc", "Sales"))
                            .build();

                    // Build products list
                    List<ProductSearchDTO.ProductSummaryDTO> products = List.of();
                    if (itemsData.getContent() != null) {
                        products = itemsData.getContent().stream()
                                .filter(product -> product != null && !Boolean.TRUE.equals(product.getHasError()))
                                .map(product -> {
                                    // Extract promotion percent
                                    Integer promotionPercent = null;
                                    if (product.getPromotionPricePercent() != null &&
                                        !product.getPromotionPricePercent().isEmpty()) {
                                        promotionPercent = product.getPromotionPricePercent().get(0).getPercent();
                                    }

                                    // Extract image URLs
                                    List<String> imageUrls = List.of();
                                    if (product.getPictures() != null) {
                                        imageUrls = product.getPictures().stream()
                                                .map(AliexpressProductSearchResponse.Picture::getUrl)
                                                .filter(url -> url != null && !url.isEmpty())
                                                .toList();
                                    }

                                    // Parse sales count
                                    Integer salesCount = null;
                                    try {
                                        String salesStr = product.getTotalSales();
                                        if (salesStr != null) {
                                            salesCount = Integer.parseInt(salesStr);
                                        }
                                    } catch (NumberFormatException e) {
                                        log.debug("Could not parse sales count: {}", product.getTotalSales());
                                    }

                                    // Get prices
                                    String currentPrice = "N/A";
                                    String originalPrice = null;
                                    String currencySign = "$";
                                    boolean hasDiscount = false;

                                    if (product.getPrice() != null) {
                                        currencySign = product.getPrice().getCurrencySign() != null ?
                                                      product.getPrice().getCurrencySign() : "$";

                                        if (product.hasPromotion()) {
                                            currentPrice = String.format("%.2f",
                                                    product.getPromotionPrice().getOriginalPrice());
                                            originalPrice = String.format("%.2f",
                                                    product.getPrice().getOriginalPrice());
                                            hasDiscount = true;
                                        } else if (product.getPrice().getConvertedPriceWithoutSign() != null) {
                                            currentPrice = product.getPrice().getConvertedPriceWithoutSign();
                                        } else if (product.getPrice().getOriginalPrice() != null) {
                                            currentPrice = String.format("%.2f",
                                                    product.getPrice().getOriginalPrice());
                                        }
                                    }

                                    return ProductSearchDTO.ProductSummaryDTO.builder()
                                            .itemId(product.getId())
                                            .itemIdNumeric(product.getItemIdNumeric())
                                            .title(product.getTitle())
                                            .imageUrl(product.getMainPictureUrl())
                                            .productUrl(product.getExternalItemUrl())
                                            .currentPrice(currentPrice)
                                            .originalPrice(originalPrice)
                                            .currencySign(currencySign)
                                            .salesCount(salesCount)
                                            .hasDiscount(hasDiscount)
                                            .rating(product.getRating())
                                            .reviewCount(product.getReviewCount())
                                            .vendorName(product.getVendorDisplayName())
                                            .brandName(product.getBrandName())
                                            .promotionPercent(promotionPercent)
                                            .imageUrls(imageUrls)
                                            .build();
                                })
                                .toList();
                    }

                    return ProductSearchDTO.builder()
                            .meta(meta)
                            .products(products)
                            .build();
                })
                .doOnError(error ->
                        log.error("Error building simplified DTO from NEW API: {}", error.getMessage()));
    }

    /**
     * Get marketplace name
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public String getMarketplaceName() {
        return "AliExpress";
    }
}