package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ProductSearchDTO;
import com.aptech.aptechMall.model.m1688.m1688ProductSearchResponse;
import com.aptech.aptechMall.model.m1688.m1688ProductDetailResponse;
import com.aptech.aptechMall.service.m1688Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Alibaba 1688 marketplace operations
 * Provides endpoints for searching products and retrieving product details from 1688
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:4200"})
public class Alibaba1688Controller {

    private final m1688Service alibaba1688Service;

    /**
     * Search 1688 products using BatchSearchItemsFrame (Full Response)
     * GET /api/1688/search?keyword=联想&language=en&page=1&pageSize=12
     */
    @GetMapping("/1688/search")
    public Mono<ResponseEntity<m1688ProductSearchResponse>> searchProducts1688(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {

        // Convert page number to framePosition (offset)
        int framePosition = (page - 1) * pageSize;

        log.info("Received 1688 search request - keyword: {}, language: {}, page: {}, pageSize: {}, framePosition: {}",
                 keyword, language, page, pageSize, framePosition);

        return alibaba1688Service.searchProducts1688API(keyword, language, framePosition, pageSize)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in 1688 search controller: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Search 1688 products (Simplified Response)
     * GET /api/1688/search/simple?keyword=联想&language=en&page=1&pageSize=12
     *
     * RECOMMENDED: Use this endpoint for frontend integration
     */
    @GetMapping("/1688/search/simple")
    public Mono<ResponseEntity<ProductSearchDTO>> searchProducts1688Simple(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {

        // Convert page number to framePosition (offset)
        int framePosition = (page - 1) * pageSize;

        log.info("Received 1688 simplified search request - keyword: {}, language: {}, page: {}, pageSize: {}, framePosition: {}",
                 keyword, language, page, pageSize, framePosition);

        return alibaba1688Service.searchProductsSimplified(keyword, language, framePosition, pageSize)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in 1688 simplified search controller: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Alternative endpoint: Search with page-based pagination
     * GET /api/1688/search/page?keyword=联想&page=1&pageSize=10
     */
    @GetMapping("/1688/search/page")
    public Mono<ResponseEntity<ProductSearchDTO>> searchByPage(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        log.info("Received 1688 page-based search request - keyword: {}, page: {}, pageSize: {}",
                 keyword, page, pageSize);

        // Convert page to framePosition
        int framePosition = (page - 1) * pageSize;

        return alibaba1688Service.searchProductsSimplified(keyword, "en", framePosition, pageSize)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in 1688 page-based search: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get 1688 product details (Full response)
     * GET /api/1688/products/{productId}
     *
     * Example: GET /api/1688/products/802318698033
     * Note: Use numeric ID without "abb-" prefix
     */
    @GetMapping("/1688/products/{productId}")
    public Mono<ResponseEntity<m1688ProductDetailResponse>> getProductDetails1688(
            @PathVariable String productId) {

        log.info("Received 1688 product details request for ID: {}", productId);

        return alibaba1688Service.getProductDetailsFull(productId)
                .map(product -> {
                    log.info("Successfully retrieved 1688 product details for ID: {}", productId);
                    return ResponseEntity.ok(product);
                })
                .onErrorResume(error -> {
                    log.error("Error in 1688 product details controller for ID {}: {}", productId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get 1688 product reviews
     * GET /api/1688/products/{productId}/reviews?page=1
     */
    @GetMapping("/1688/products/{productId}/reviews")
    public Mono<ResponseEntity<String>> getProductReviews1688(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int page) {

        log.info("Received 1688 reviews request for product ID: {}, page: {}", productId, page);

        return alibaba1688Service.getProductReviews(productId, page)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in 1688 reviews controller for ID {}: {}", productId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint for 1688 service
     * GET /api/1688/health
     */
    @GetMapping("/1688/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Alibaba 1688 API");
        response.put("marketplace", alibaba1688Service.getMarketplaceName());
        return ResponseEntity.ok(response);
    }

    /**
     * Get marketplace information
     * GET /api/1688/info
     */
    @GetMapping("/1688/info")
    public ResponseEntity<Map<String, Object>> getMarketplaceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("marketplace", alibaba1688Service.getMarketplaceName());
        info.put("description", "Alibaba 1688 is China's leading wholesale marketplace");
        info.put("currency", "CNY (¥)");
        info.put("language", "Chinese (Simplified)");
        info.put("baseUrl", "https://www.1688.com");
        info.put("endpoints", Map.of(
            "search", "/api/1688/search/simple?keyword={keyword}",
            "productDetails", "/api/1688/products/{productId}",
            "reviews", "/api/1688/products/{productId}/reviews?page={page}"
        ));
        return ResponseEntity.ok(info);
    }
}
