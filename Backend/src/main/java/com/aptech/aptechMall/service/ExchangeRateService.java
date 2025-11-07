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

/**
 * Service quản lý tỷ giá ngoại tệ (Exchange Rate Management)
 *
 * Chức năng chính:
 * - Lấy tỷ giá từ RapidAPI (USD/CNY → VND)
 * - Cache tỷ giá trong database
 * - Cung cấp fallback rates khi API không available
 * - Convert tiền tệ giữa các loại (USD, CNY, VND)
 *
 * TỶ GIÁ CẦN THIẾT:
 * - **USD → VND**: Cho sản phẩm AliExpress (marketplace = ALIEXPRESS)
 * - **CNY → VND**: Cho sản phẩm 1688 (marketplace = ALIBABA1688)
 *
 * NGUỒN TỶ GIÁ (Priority order):
 * 1. **RAPIDAPI**: Tỷ giá real-time từ RapidAPI Exchange Rate API
 * 2. **MANUAL**: Admin cập nhật thủ công (override API rate)
 * 3. **FALLBACK**: Hardcoded values khi API không available
 *
 * FALLBACK RATES (Conservative estimates):
 * - USD: 25,000 VND (thực tế ~24,000-25,000)
 * - CNY: 3,500 VND (thực tế ~3,400-3,600)
 * - Rates này cao hơn thực tế một chút để tránh loss
 *
 * INITIALIZATION (@PostConstruct):
 * - Khi application start, check database có tỷ giá chưa
 * - Nếu database rỗng → fetch từ RapidAPI ngay lập tức
 * - Nếu đã có tỷ giá → skip (dùng rates hiện tại)
 *
 * CACHING STRATEGY:
 * - Tỷ giá được lưu trong database (ExchangeRate entity)
 * - Không tự động update định kỳ (phải manual trigger)
 * - Tỷ giá ổn định → không cần update thường xuyên
 * - Admin có thể manual update khi cần
 *
 * CURRENCY CONVERSION:
 * - Hỗ trợ convert giữa USD, CNY, VND
 * - Luôn convert qua VND làm intermediate
 * - Ví dụ: USD → CNY = USD → VND → CNY
 *
 * ROUNDING:
 * - VND: Làm tròn về số nguyên (HALF_UP)
 * - USD/CNY: 2 chữ số thập phân
 *
 * USE CASES:
 *
 * 1. **Checkout (OrderService)**:
 *    - User checkout giỏ hàng có sản phẩm AliExpress (USD)
 *    - System lấy tỷ giá USD → VND
 *    - Convert item prices sang VND để tính tổng
 *
 * 2. **Fee Calculation (FeeCalculationService)**:
 *    - Wooden packaging fee: 20 CNY → VND
 *    - Bubble wrap fee: 10 CNY → VND
 *    - Accessory detection: USD → CNY để so sánh với 10 CNY threshold
 *
 * 3. **Admin Fee Update (OrderService.updateOrderFees)**:
 *    - Admin nhập domestic shipping fee bằng CNY
 *    - System convert CNY → VND để cộng vào total amount
 *
 * RAPIDAPI CONFIGURATION:
 * - API Key: Lưu trong application.properties
 * - Host: currency-conversion-and-exchange-rates.p.rapidapi.com
 * - Base URL: https://currency-conversion-and-exchange-rates.p.rapidapi.com
 * - Endpoint: /convert?base={from}&target={to}
 * - Free tier: 100 requests/month (đủ cho cache strategy)
 *
 * ERROR HANDLING:
 * - Nếu RapidAPI fail → log error và tiếp tục (không crash app)
 * - Nếu database không có rate → dùng fallback rate
 * - Fallback rate đảm bảo system luôn hoạt động được
 */
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

    /**
     * Lấy tỷ giá của một loại tiền tệ
     *
     * Tìm trong database trước, nếu không có → dùng fallback rate
     *
     * @param currency Mã tiền tệ (USD, CNY)
     * @return ExchangeRateResponse với rate, source, updatedAt
     */
    public ExchangeRateResponse getRate(String currency) {
        return exchangeRateRepository
            .findByCurrency(currency.toUpperCase())
            .map(rate -> ExchangeRateResponse.builder()
                    .currency(rate.getCurrency())
                    .rateToVnd(rate.getRateToVnd())
                    .source(rate.getSource())
                    .updatedAt(rate.getUpdatedAt())
                    .build())
            .orElseGet(() -> getFallbackRate(currency));
    }

    /**
     * Get fallback exchange rate if database rate not available
     * @param currency Currency code
     * @return Fallback exchange rate
     */
    private ExchangeRateResponse getFallbackRate(String currency) {
        log.warn("⚠️ Using fallback exchange rate for {}", currency);

        // Default fallback rates (conservative estimates)
        BigDecimal defaultRate;
        switch (currency.toUpperCase()) {
            case "USD":
                defaultRate = BigDecimal.valueOf(25000);
                break;
            case "CNY":
                defaultRate = BigDecimal.valueOf(3500);
                break;
            default:
                log.error("❌ No fallback rate available for currency: {}", currency);
                throw new RuntimeException(
                    "Exchange rate not available for: " + currency);
        }

        return ExchangeRateResponse.builder()
                .currency(currency.toUpperCase())
                .rateToVnd(defaultRate)
                .source("FALLBACK")
                .updatedAt(java.time.LocalDateTime.now())
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

    /**
     * Convert số tiền từ loại tiền này sang loại tiền khác
     *
     * CONVERSION LOGIC:
     * - Nếu cùng currency → return nguyên amount (no conversion)
     * - fromCurrency → VND: Nhân với rate
     * - VND → toCurrency: Chia cho rate
     * - fromCurrency → toCurrency: Convert qua VND (fromCurrency → VND → toCurrency)
     *
     * VÍ DỤ:
     * 1. USD → VND:
     *    - 100 USD x 25,000 = 2,500,000 VND
     *
     * 2. CNY → VND:
     *    - 100 CNY x 3,500 = 350,000 VND
     *
     * 3. USD → CNY (qua VND):
     *    - 100 USD → 2,500,000 VND
     *    - 2,500,000 VND ÷ 3,500 = 714.29 CNY
     *
     * ROUNDING:
     * - Target = VND: Làm tròn về số nguyên (HALF_UP)
     * - Target = USD/CNY: 2 chữ số thập phân
     *
     * USE CASES:
     * - OrderService.checkout(): USD/CNY → VND để tính tổng đơn hàng
     * - FeeCalculationService: CNY → VND cho packaging fees
     * - FeeCalculationService: USD → CNY cho accessory detection
     *
     * @param amount Số tiền cần convert
     * @param fromCurrency Loại tiền nguồn (USD, CNY, VND)
     * @param toCurrency Loại tiền đích (USD, CNY, VND)
     * @return Số tiền sau khi convert
     */
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // If same currency, no conversion needed
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        // Get exchange rate for source currency to VND
        ExchangeRateResponse fromRate = getRate(fromCurrency);

        // If target is VND, just multiply by rate
        if ("VND".equalsIgnoreCase(toCurrency)) {
            return amount.multiply(fromRate.getRateToVnd())
                    .setScale(0, RoundingMode.HALF_UP);
        }

        // If source is VND, divide by target rate
        if ("VND".equalsIgnoreCase(fromCurrency)) {
            ExchangeRateResponse toRate = getRate(toCurrency);
            return amount.divide(toRate.getRateToVnd(), 2, RoundingMode.HALF_UP);
        }

        // Convert through VND (fromCurrency -> VND -> toCurrency)
        BigDecimal amountInVND = amount.multiply(fromRate.getRateToVnd());
        ExchangeRateResponse toRate = getRate(toCurrency);
        return amountInVND.divide(toRate.getRateToVnd(), 2, RoundingMode.HALF_UP);
    }
}
