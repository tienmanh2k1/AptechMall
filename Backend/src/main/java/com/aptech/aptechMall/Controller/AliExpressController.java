package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ProductDetailDTO;
import com.aptech.aptechMall.dto.ProductSearchDTO;
import com.aptech.aptechMall.model.Aliexpress.AliexpressProductSearchResponse;
import com.aptech.aptechMall.model.Aliexpress.AliexpressProductDetailResponse;
import com.aptech.aptechMall.service.AliExpressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AliExpressController {

    private final AliExpressService aliExpressService;

    /**
     * Search products using BatchSearchItemsFrame (Full Response)
     * GET /api/aliexpress/search?keyword=iphone&language=en&page=1&pageSize=12
     */
    @GetMapping("/aliexpress/search")
    public Mono<ResponseEntity<AliexpressProductSearchResponse>> searchProductsAliExpress(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {

        // Convert page number to framePosition (offset)
        int framePosition = (page - 1) * pageSize;

        log.info("Received search request (BatchSearchItemsFrame) - keyword: {}, language: {}, page: {}, pageSize: {}, framePosition: {}",
                 keyword, language, page, pageSize, framePosition);

        return aliExpressService.searchProductsNewAPI(keyword, language, framePosition, pageSize)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in search controller: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Search products (Simplified Response) - BatchSearchItemsFrame
     * GET /api/aliexpress/search/simple?keyword=iphone&language=en&page=1&pageSize=12
     */
    @GetMapping("/aliexpress/search/simple")
    public Mono<ResponseEntity<ProductSearchDTO>> searchProductsSimpleAliExpress(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {

        // Convert page number to framePosition (offset)
        int framePosition = (page - 1) * pageSize;

        log.info("Received simplified search request (BatchSearchItemsFrame) - keyword: {}, language: {}, page: {}, pageSize: {}, framePosition: {}",
                 keyword, language, page, pageSize, framePosition);

        return aliExpressService.searchProductsSimplified(keyword, language, framePosition, pageSize)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in simplified search controller: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get product details (Full response) - NEW OtAPI FORMAT
     * GET /api/aliexpress/products/1005005244562338
     */
    @GetMapping("/aliexpress/products/{productId}")
    public Mono<ResponseEntity<AliexpressProductDetailResponse>> getProductDetails(
            @PathVariable String productId) {

        log.info("Received product details request (OtAPI) for ID: {}", productId);

        return aliExpressService.getProductDetailsFull(productId)
                .map(product -> {
                    log.info("Successfully retrieved product details (OtAPI) for ID: {}", productId);
                    return ResponseEntity.ok(product);
                })
                .onErrorResume(error -> {
                    log.error("Error in product details controller (OtAPI) for ID {}: {}", productId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get simplified product details
     * GET /api/aliexpress/products/1005005244562338/simple
     */
    @GetMapping("/aliexpress/products/{productId}/simple")
    public Mono<ResponseEntity<ProductDetailDTO>> getProductSimple(
            @PathVariable String productId) {

        log.info("Received simplified product details request for ID: {}", productId);

        return aliExpressService.getProductDetails(productId)
                .map(product -> {
                    log.info("Successfully retrieved simplified product details for ID: {}", productId);
                    return ResponseEntity.ok(product);
                })
                .onErrorResume(error -> {
                    log.error("Error in simplified product controller for ID {}: {}", productId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get product reviews
     * GET /api/aliexpress/products/1005005244562338/reviews?page=1
     */
    @GetMapping("/aliexpress/products/{productId}/reviews")
    public Mono<ResponseEntity<String>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int page) {

        log.info("Received reviews request for product ID: {}, page: {}", productId, page);

        return aliExpressService.getProductReviews(productId, page)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in reviews controller for ID {}: {}", productId, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint
     * GET /api/aliexpress/health
     */
    @GetMapping("/aliexpress/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AliExpress API");
        return ResponseEntity.ok(response);
    }
}