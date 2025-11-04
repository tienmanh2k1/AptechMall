package com.aptech.aptechMall.service;

import com.aptech.aptechMall.dto.exchange.RapidApiExchangeRateResponse;
import com.aptech.aptechMall.dto.exchange.ExchangeRateResponse;
import com.aptech.aptechMall.entity.ExchangeRate;
import com.aptech.aptechMall.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rapidapi.exchange.key}")
    private String apiKey;

    @Value("${rapidapi.exchange.host}")
    private String apiHost;

    @Value("${rapidapi.exchange.base-url}")
    private String baseUrl;

    public Map<String, ExchangeRateResponse> getAllRates() {
        List<ExchangeRate> rates = exchangeRateRepository.findAll();

        return rates.stream()
                .collect(Collectors.toMap(
                        ExchangeRate::getCurrency,
                        rate -> ExchangeRateResponse.builder()
                                .currency(rate.getCurrency())
                                .rateToVnd(rate.getRateToVnd())
                                .source(rate.getSource())
                                .updatedAt(rate.getUpdatedAt())
                                .build()
                ));
    }

    public ExchangeRateResponse getRate(String currency) {
        ExchangeRate rate = exchangeRateRepository
            .findByCurrency(currency.toUpperCase())
            .orElseThrow(() -> new RuntimeException(
                "Exchange rate not found for: " + currency));

        return ExchangeRateResponse.builder()
                .currency(rate.getCurrency())
                .rateToVnd(rate.getRateToVnd())
                .source(rate.getSource())
                .updatedAt(rate.getUpdatedAt())
                .build();
    }

    @PostConstruct
    public void initializeRates() {
        long count = exchangeRateRepository.count();
        if (count == 0) {
            log.info("🔄 No exchange rates found. Fetching initial rates...");
            updateRatesFromApi();
        } else {
            log.info("✅ Exchange rates already exist ({} currencies)", count);
        }
    }

    @Transactional
    public void updateRatesFromApi() {
        try {
            log.info("📊 Fetching exchange rates from RapidAPI...");

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key", apiKey);
            headers.set("x-rapidapi-host", apiHost);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Fetch USD to VND
            String usdUrl = baseUrl + "/convert?base=USD&target=VND";
            ResponseEntity<RapidApiExchangeRateResponse> usdResponse =
                restTemplate.exchange(
                    usdUrl,
                    HttpMethod.GET,
                    entity,
                    RapidApiExchangeRateResponse.class
                );

            if (usdResponse.getBody() != null &&
                "0".equals(usdResponse.getBody().getCode()) &&
                usdResponse.getBody().getConvertResult() != null) {

                double usdToVnd = usdResponse.getBody()
                    .getConvertResult().getRate();

                updateOrCreateRate("USD",
                    BigDecimal.valueOf(usdToVnd),
                    "RAPIDAPI");
                log.info("✅ Updated USD rate: {} VND", usdToVnd);
            }

            // Fetch CNY to VND
            String cnyUrl = baseUrl + "/convert?base=CNY&target=VND";
            ResponseEntity<RapidApiExchangeRateResponse> cnyResponse =
                restTemplate.exchange(
                    cnyUrl,
                    HttpMethod.GET,
                    entity,
                    RapidApiExchangeRateResponse.class
                );

            if (cnyResponse.getBody() != null &&
                "0".equals(cnyResponse.getBody().getCode()) &&
                cnyResponse.getBody().getConvertResult() != null) {

                double cnyToVnd = cnyResponse.getBody()
                    .getConvertResult().getRate();

                updateOrCreateRate("CNY",
                    BigDecimal.valueOf(cnyToVnd)
                        .setScale(2, RoundingMode.HALF_UP),
                    "RAPIDAPI");
                log.info("✅ Updated CNY rate: {} VND", cnyToVnd);
            }

            log.info("✅ Exchange rates updated successfully from RapidAPI");

        } catch (Exception e) {
            log.error("❌ Failed to update exchange rates: {}",
                e.getMessage(), e);
        }
    }

    @Transactional
    public void updateOrCreateRate(String currency,
                                   BigDecimal rateToVnd,
                                   String source) {
        ExchangeRate rate = exchangeRateRepository
            .findByCurrency(currency)
            .orElse(ExchangeRate.builder()
                    .currency(currency)
                    .build());

        rate.setRateToVnd(rateToVnd);
        rate.setSource(source);
        exchangeRateRepository.save(rate);
    }

    @Transactional
    public ExchangeRateResponse manualUpdateRate(String currency,
                                                 BigDecimal rateToVnd) {
        log.info("📝 Manual update: {} = {} VND", currency, rateToVnd);
        updateOrCreateRate(currency.toUpperCase(), rateToVnd, "MANUAL");
        return getRate(currency);
    }
}
