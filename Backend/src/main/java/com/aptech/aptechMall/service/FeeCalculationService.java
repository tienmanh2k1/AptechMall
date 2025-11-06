package com.aptech.aptechMall.service;

import com.aptech.aptechMall.entity.Marketplace;
import com.aptech.aptechMall.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for calculating order fees
 * Implements PandaMall fee structure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationService {

    private final ExchangeRateService exchangeRateService;

    // Service fee is 1.5% of product cost
    private static final BigDecimal SERVICE_FEE_PERCENTAGE = new BigDecimal("1.5");

    // Item count check fee (VND per item)
    private static final BigDecimal ITEM_COUNT_FEE_1_5 = new BigDecimal("5000");
    private static final BigDecimal ITEM_COUNT_FEE_6_20 = new BigDecimal("3000");
    private static final BigDecimal ITEM_COUNT_FEE_21_100 = new BigDecimal("2000");
    private static final BigDecimal ITEM_COUNT_FEE_101_500 = new BigDecimal("1500");
    private static final BigDecimal ITEM_COUNT_FEE_501_10000 = new BigDecimal("1000");

    // Accessory item (price < 10 CNY) count check fee
    private static final BigDecimal ACCESSORY_FEE_1_5 = new BigDecimal("2500");
    private static final BigDecimal ACCESSORY_FEE_6_20 = new BigDecimal("2000");
    private static final BigDecimal ACCESSORY_FEE_21_100 = new BigDecimal("1500");
    private static final BigDecimal ACCESSORY_FEE_101_500 = new BigDecimal("1000");
    private static final BigDecimal ACCESSORY_FEE_501_10000 = new BigDecimal("800");

    // Accessory threshold (CNY)
    private static final BigDecimal ACCESSORY_PRICE_THRESHOLD = new BigDecimal("10");

    // Wooden packaging fee (CNY)
    private static final BigDecimal WOODEN_PACKAGING_FIRST_KG = new BigDecimal("20");
    private static final BigDecimal WOODEN_PACKAGING_ADDITIONAL_KG = new BigDecimal("1");

    // Bubble wrap packaging fee (CNY)
    private static final BigDecimal BUBBLE_WRAP_FIRST_KG = new BigDecimal("10");
    private static final BigDecimal BUBBLE_WRAP_ADDITIONAL_KG = new BigDecimal("1.5");

    /**
     * Calculate service fee (1.5% of product cost)
     * @param productCost Total product cost in VND
     * @return Service fee in VND
     */
    public BigDecimal calculateServiceFee(BigDecimal productCost) {
        if (productCost == null || productCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return productCost
                .multiply(SERVICE_FEE_PERCENTAGE)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate item count check fee based on number of items
     * @param orderItems List of order items
     * @return Item count check fee in VND
     */
    public BigDecimal calculateItemCountCheckFee(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int totalItems = 0;
        int accessoryItems = 0;

        // Count total items and accessory items
        for (OrderItem item : orderItems) {
            int quantity = item.getQuantity();
            totalItems += quantity;

            // Check if item is accessory (price < 10 CNY)
            BigDecimal priceInCNY = item.getPrice();
            String currency = inferCurrency(item.getMarketplace());

            // Convert to CNY if needed
            if (!"CNY".equals(currency)) {
                priceInCNY = exchangeRateService.convertCurrency(
                        priceInCNY,
                        currency,
                        "CNY"
                );
            }

            if (priceInCNY != null && priceInCNY.compareTo(ACCESSORY_PRICE_THRESHOLD) < 0) {
                accessoryItems += quantity;
            }
        }

        int regularItems = totalItems - accessoryItems;

        // Calculate fee for regular items
        BigDecimal regularFee = calculateFeeByQuantity(regularItems, false);

        // Calculate fee for accessory items
        BigDecimal accessoryFee = calculateFeeByQuantity(accessoryItems, true);

        log.info("Item count check fee calculated: {} regular items ({}đ), {} accessory items ({}đ), total: {}đ",
                regularItems, regularFee, accessoryItems, accessoryFee, regularFee.add(accessoryFee));

        return regularFee.add(accessoryFee);
    }

    /**
     * Infer currency from marketplace
     * @param marketplace Marketplace enum
     * @return Currency code (USD for ALIEXPRESS, CNY for ALIBABA1688)
     */
    private String inferCurrency(Marketplace marketplace) {
        if (marketplace == null) {
            return "USD"; // Default to USD
        }

        if (marketplace == Marketplace.ALIEXPRESS) {
            return "USD";
        } else if (marketplace == Marketplace.ALIBABA1688) {
            return "CNY";
        }

        return "USD"; // Default fallback
    }

    /**
     * Calculate fee based on quantity tier
     * @param quantity Number of items
     * @param isAccessory True if accessory items
     * @return Fee in VND
     */
    private BigDecimal calculateFeeByQuantity(int quantity, boolean isAccessory) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal feePerItem;

        if (quantity >= 501) {
            feePerItem = isAccessory ? ACCESSORY_FEE_501_10000 : ITEM_COUNT_FEE_501_10000;
        } else if (quantity >= 101) {
            feePerItem = isAccessory ? ACCESSORY_FEE_101_500 : ITEM_COUNT_FEE_101_500;
        } else if (quantity >= 21) {
            feePerItem = isAccessory ? ACCESSORY_FEE_21_100 : ITEM_COUNT_FEE_21_100;
        } else if (quantity >= 6) {
            feePerItem = isAccessory ? ACCESSORY_FEE_6_20 : ITEM_COUNT_FEE_6_20;
        } else {
            feePerItem = isAccessory ? ACCESSORY_FEE_1_5 : ITEM_COUNT_FEE_1_5;
        }

        return feePerItem.multiply(new BigDecimal(quantity));
    }

    /**
     * Calculate wooden packaging fee based on weight
     * @param weightKg Weight in kilograms
     * @return Wooden packaging fee in VND
     */
    public BigDecimal calculateWoodenPackagingFee(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // First kg: 20 CNY
        BigDecimal fee = WOODEN_PACKAGING_FIRST_KG;

        // Additional kg: 1 CNY per kg
        if (weightKg.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal additionalKg = weightKg.subtract(BigDecimal.ONE);
            fee = fee.add(additionalKg.multiply(WOODEN_PACKAGING_ADDITIONAL_KG));
        }

        // Convert CNY to VND
        BigDecimal feeInVND = exchangeRateService.convertCurrency(fee, "CNY", "VND");

        log.info("Wooden packaging fee calculated: {}kg = {} CNY = {}đ", weightKg, fee, feeInVND);

        return feeInVND;
    }

    /**
     * Calculate bubble wrap packaging fee based on weight
     * @param weightKg Weight in kilograms
     * @return Bubble wrap fee in VND
     */
    public BigDecimal calculateBubbleWrapFee(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // First kg: 10 CNY
        BigDecimal fee = BUBBLE_WRAP_FIRST_KG;

        // Additional kg: 1.5 CNY per kg
        if (weightKg.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal additionalKg = weightKg.subtract(BigDecimal.ONE);
            fee = fee.add(additionalKg.multiply(BUBBLE_WRAP_ADDITIONAL_KG));
        }

        // Convert CNY to VND
        BigDecimal feeInVND = exchangeRateService.convertCurrency(fee, "CNY", "VND");

        log.info("Bubble wrap fee calculated: {}kg = {} CNY = {}đ", weightKg, fee, feeInVND);

        return feeInVND;
    }

    /**
     * Calculate total additional services fee
     * @param orderItems List of order items (for item count)
     * @param weightKg Weight for packaging services
     * @param includeWoodenPackaging Include wooden packaging
     * @param includeBubbleWrap Include bubble wrap
     * @param includeItemCountCheck Include item count check
     * @return Total additional services fee in VND
     */
    public BigDecimal calculateTotalAdditionalServicesFee(
            List<OrderItem> orderItems,
            BigDecimal weightKg,
            boolean includeWoodenPackaging,
            boolean includeBubbleWrap,
            boolean includeItemCountCheck
    ) {
        BigDecimal total = BigDecimal.ZERO;

        if (includeItemCountCheck) {
            total = total.add(calculateItemCountCheckFee(orderItems));
        }

        if (includeWoodenPackaging) {
            total = total.add(calculateWoodenPackagingFee(weightKg));
        }

        if (includeBubbleWrap) {
            total = total.add(calculateBubbleWrapFee(weightKg));
        }

        return total;
    }
}
