package com.aptech.aptechMall.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DebugController {

    @Value("${rapidapi.aliexpress.key}")
    private String apiKey;

    @Value("${rapidapi.aliexpress.host}")
    private String apiHost;

    @Value("${rapidapi.aliexpress.base-url}")
    private String baseUrl;

    /**
     * Get raw JSON response for debugging
     * GET /api/debug/raw/1005010133081414
     */
    @GetMapping("/raw/{productId}")
    public Mono<ResponseEntity<String>> getRawJson(@PathVariable String productId) {
        log.info("Getting raw JSON for product ID: {}", productId);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/item_detail_2")
                        .queryParam("itemId", productId)
                        .build())
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", apiHost)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    log.info("Raw JSON received (first 1000 chars): {}",
                            json.substring(0, Math.min(1000, json.length())));
                    return ResponseEntity.ok()
                            .header("Content-Type", "application/json")
                            .body(json);
                })
                .onErrorResume(error -> {
                    log.error("Error getting raw JSON: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("{\"error\": \"" + error.getMessage() + "\"}"));
                });
    }
}