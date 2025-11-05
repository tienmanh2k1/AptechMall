package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.entity.PaymentGateway;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.service.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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

    private final WalletService walletService;
    private final UserRepository userRepository;

    /**
     * Debug endpoint to check current authentication
     * GET /api/debug/auth
     */
    @GetMapping("/auth")
    public ResponseEntity<Object> debugAuth() {
        try {
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null) {
                return ResponseEntity.ok(java.util.Map.of(
                    "authenticated", false,
                    "message", "No authentication found"
                ));
            }

            return ResponseEntity.ok(java.util.Map.of(
                "authenticated", auth.isAuthenticated(),
                "principal", auth.getPrincipal().getClass().getSimpleName(),
                "authorities", auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(java.util.stream.Collectors.toList()),
                "name", auth.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
        }
    }

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

    /**
     * Add test funds to a user's wallet (for testing only)
     * POST /api/debug/wallet/add-funds?email=demo.account@gmail.com&amount=100000
     */
    @PostMapping("/wallet/add-funds")
    public ResponseEntity<Object> addTestFunds(
            @RequestParam String email,
            @RequestParam BigDecimal amount) {
        try {
            log.info("DEBUG: Adding {} to wallet for user: {}", amount, email);

            // Find user by email
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // Process deposit using WalletService
            var transaction = walletService.processDeposit(
                    user.getUserId(),
                    amount,
                    PaymentGateway.BANK_TRANSFER,
                    "TEST-" + System.currentTimeMillis()
            );

            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Added " + amount + " VND to wallet for " + email,
                    "transaction", transaction
            ));
        } catch (Exception e) {
            log.error("Error adding test funds: {}", e.getMessage(), e);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            ));
        }
    }
}