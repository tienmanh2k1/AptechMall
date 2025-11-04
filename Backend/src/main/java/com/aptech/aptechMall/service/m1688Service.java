package com.aptech.aptechMall.service;

import com.aptech.aptechMall.dto.ProductDetailDTO;
import com.aptech.aptechMall.dto.ProductSearchDTO;
import com.aptech.aptechMall.model.m1688.m1688ProductSearchResponse;
import com.aptech.aptechMall.model.m1688.m1688ProductDetailResponse;
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

/**
 * Service for Alibaba 1688 marketplace integration via RapidAPI
 * Implements ProductMarketplaceService for consistency with other marketplace services
 */
@Slf4j
@Service
public class m1688Service implements ProductMarketplaceService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${rapidapi.m1688.key}")
    private String apiKey;

    @Value("${rapidapi.m1688.host}")
    private String apiHost;

    public m1688Service(
            @Value("${rapidapi.m1688.base-url}") String baseUrl,
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

        log.info("Alibaba1688Service: WebClient initialized with 10MB buffer size");
    }

    /**
     * Search products using BatchSearchItemsFrame API (1688 format)
     */
    public Mono<m1688ProductSearchResponse> searchProducts1688API(String keyword, String language,
                                                                    int framePosition, int frameSize) {
        log.info("Searching 1688 products - keyword: {}, language: {}, framePosition: {}, frameSize: {}",
                keyword, language, framePosition, frameSize);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/BatchSearchItemsFrame")
                        .queryParam("ItemTitle", keyword)  // Changed from "keyword" to "ItemTitle"
                        .queryParam("language", language)
                        .queryParam("framePosition", framePosition)
                        .queryParam("frameSize", frameSize)
                        .build())
                .header("x-rapidapi-key", apiKey)  // Changed to lowercase
                .header("x-rapidapi-host", apiHost)  // Changed to lowercase
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Error response from 1688 API: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Error body: {}", errorBody);
                                return Mono.error(new RuntimeException("1688 API Error: " + errorBody));
                            });
                })
                .bodyToMono(String.class)
                .doOnNext(jsonString -> {
                    log.debug("Raw JSON response from 1688 API: {}",
                            jsonString.substring(0, Math.min(500, jsonString.length())) + "...");
                })
                .flatMap(jsonString -> {
                    try {
                        m1688ProductSearchResponse response = objectMapper.readValue(
                                jsonString, m1688ProductSearchResponse.class);
                        log.info("Successfully parsed 1688 search results - ErrorCode: {}",
                                response.getErrorCode());
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Error parsing 1688 JSON: {}", e.getMessage());
                        log.error("Problematic JSON: {}", jsonString);
                        return Mono.error(new RuntimeException("JSON parsing error: " + e.getMessage(), e));
                    }
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(WebClientResponseException.class, error ->
                        log.error("WebClient error (1688): Status={}, Body={}",
                                error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(error ->
                        log.error("Error searching 1688 products: {}", error.getMessage(), error));
    }

    /**
     * Search products and return simplified DTO
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public Mono<ProductSearchDTO> searchProducts(String keyword, int page, int sort) {
        log.info("searchProducts called for 1688 - keyword: {}, page: {}", keyword, page);

        // Convert page to framePosition
        int frameSize = 10;
        int framePosition = (page - 1) * frameSize;

        return searchProductsSimplified(keyword, "en", framePosition, frameSize);
    }

    /**
     * Search products using 1688 API and return simplified DTO
     */
    public Mono<ProductSearchDTO> searchProductsSimplified(String keyword, String language,
                                                            int framePosition, int frameSize) {
        return searchProducts1688API(keyword, language, framePosition, frameSize)
                .map(response -> {
                    if (response == null ||
                        response.getResult() == null ||
                        response.getResult().getItems() == null ||
                        response.getResult().getItems().getItems() == null) {
                        log.warn("Invalid response structure from 1688 API");
                        return ProductSearchDTO.builder()
                                .products(List.of())
                                .build();
                    }

                    var itemsData = response.getResult().getItems().getItems();

                    // Build meta
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
                                                .map(m1688ProductSearchResponse.Picture::getUrl)
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
                                    String currencySign = "¥"; // 1688 uses CNY
                                    boolean hasDiscount = false;

                                    if (product.getPrice() != null) {
                                        currencySign = product.getPrice().getCurrencySign() != null ?
                                                      product.getPrice().getCurrencySign() : "¥";

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
                                            .productUrl(product.getTaobaoItemUrl() != null ?
                                                       product.getTaobaoItemUrl() :
                                                       product.getExternalItemUrl())
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
                        log.error("Error building simplified DTO from 1688 API: {}", error.getMessage()));
    }

    /**
     * Get product details by ID (full response)
     */
    public Mono<m1688ProductDetailResponse> getProductDetailsFull(String productId) {
        log.info("Getting 1688 product details for ID: {}", productId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/BatchGetItemFullInfo")
                        .queryParam("language", "en")
                        .queryParam("itemId", productId)
                        .build())
                .header("x-rapidapi-key", apiKey)
                .header("x-rapidapi-host", apiHost)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Error response from 1688 API: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Error body: {}", errorBody);
                                return Mono.error(new RuntimeException("1688 API Error: " + errorBody));
                            });
                })
                .bodyToMono(String.class)
                .doOnNext(jsonString -> {
                    log.debug("Raw JSON response for 1688 product {}: {}", productId,
                            jsonString.substring(0, Math.min(500, jsonString.length())) + "...");
                })
                .flatMap(jsonString -> {
                    try {
                        m1688ProductDetailResponse response = objectMapper.readValue(
                                jsonString, m1688ProductDetailResponse.class);

                        if (!"Ok".equalsIgnoreCase(response.getErrorCode())) {
                            log.error("1688 API returned error: {}", response.getErrorCode());
                            return Mono.error(new RuntimeException("1688 API Error: " + response.getErrorCode()));
                        }

                        log.info("Successfully parsed 1688 product details for ID: {}", productId);
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Error parsing 1688 JSON for product {}: {}", productId, e.getMessage());
                        log.error("Problematic JSON: {}", jsonString);
                        return Mono.error(new RuntimeException("JSON parsing error: " + e.getMessage(), e));
                    }
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(WebClientResponseException.class, error ->
                        log.error("WebClient error getting 1688 product details: Status={}, Body={}",
                                error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(error ->
                        log.error("Error getting 1688 product details for ID {}: {}", productId, error.getMessage(), error));
    }

    /**
     * Get product details by ID (simplified DTO)
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public Mono<ProductDetailDTO> getProductDetails(String productId) {
        log.info("getProductDetails called for 1688 product: {}", productId);

        // TODO: Implement full transformation to ProductDetailDTO
        // For now, return a basic error response
        return Mono.error(new UnsupportedOperationException(
                "1688 product details DTO transformation not yet implemented. Use getProductDetailsFull() for raw data."));
    }

    /**
     * Get product reviews
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public Mono<String> getProductReviews(String productId, int page) {
        log.info("Getting reviews for 1688 product: {}, page: {}", productId, page);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/product/reviews")
                        .queryParam("productId", productId)
                        .queryParam("page", page)
                        .build())
                .header("x-rapidapi-key", apiKey)
                .header("x-rapidapi-host", apiHost)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .doOnError(error ->
                        log.error("Error getting 1688 reviews for product {}: {}", productId, error.getMessage()));
    }

    /**
     * Get marketplace name
     * Implementation of ProductMarketplaceService interface
     */
    @Override
    public String getMarketplaceName() {
        return "Alibaba1688";
    }
}
