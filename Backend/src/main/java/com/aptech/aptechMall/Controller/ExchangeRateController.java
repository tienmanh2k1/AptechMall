package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.exchange.ExchangeRateResponse;
import com.aptech.aptechMall.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    // Get all exchange rates (PUBLIC)
    @GetMapping
    public ResponseEntity<Map<String, ExchangeRateResponse>> getAllRates() {
        return ResponseEntity.ok(exchangeRateService.getAllRates());
    }

    // Get specific currency rate (PUBLIC)
    @GetMapping("/{currency}")
    public ResponseEntity<ExchangeRateResponse> getRate(
            @PathVariable String currency) {
        return ResponseEntity.ok(exchangeRateService.getRate(currency));
    }

    // Manual update rate (ADMIN only)
    @PutMapping("/{currency}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExchangeRateResponse> updateRate(
            @PathVariable String currency,
            @RequestBody Map<String, BigDecimal> request) {

        BigDecimal rateToVnd = request.get("rateToVnd");
        ExchangeRateResponse updated =
            exchangeRateService.manualUpdateRate(currency, rateToVnd);
        return ResponseEntity.ok(updated);
    }

    // Trigger manual refresh (ADMIN only)
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> refreshRates() {
        exchangeRateService.updateRatesFromApi();
        return ResponseEntity.ok("Exchange rates refreshed successfully");
    }
}
