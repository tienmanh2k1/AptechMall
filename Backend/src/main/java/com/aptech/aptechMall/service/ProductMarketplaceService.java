package com.aptech.aptechMall.service;

import com.aptech.aptechMall.dto.ProductDetailDTO;
import com.aptech.aptechMall.dto.ProductSearchDTO;
import reactor.core.publisher.Mono;

/**
 * Common interface for product marketplace services (AliExpress, Amazon, eBay, etc.)
 * Defines standard operations for searching products, retrieving details, and getting reviews.
 *
 * All methods return reactive Mono types for asynchronous processing.
 */
public interface ProductMarketplaceService {

    /**
     * Search products by keyword with pagination and sorting
     *
     * @param keyword The search keyword
     * @param page The page number (starting from 1)
     * @param sort The sort option (marketplace-specific)
     * @return Mono containing simplified search results
     */
    Mono<ProductSearchDTO> searchProducts(String keyword, int page, int sort);

    /**
     * Get detailed product information by product ID
     *
     * @param productId The unique product identifier
     * @return Mono containing simplified product details
     */
    Mono<ProductDetailDTO> getProductDetails(String productId);

    /**
     * Get product reviews with pagination
     *
     * @param productId The unique product identifier
     * @param page The page number for reviews
     * @return Mono containing review data (format may vary by marketplace)
     */
    Mono<String> getProductReviews(String productId, int page);

    /**
     * Get the marketplace name/identifier
     *
     * @return The name of the marketplace (e.g., "AliExpress", "Amazon", "eBay")
     */
    String getMarketplaceName();

    /**
     * Check if the marketplace service is healthy and API is reachable
     *
     * @return Mono containing health status message
     */
    default Mono<String> healthCheck() {
        return Mono.just(getMarketplaceName() + " service is running");
    }
}
