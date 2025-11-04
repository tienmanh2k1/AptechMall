package com.aptech.aptechMall.util;

import com.aptech.aptechMall.entity.Marketplace;

/**
 * Utility class for currency operations
 */
public class CurrencyUtil {

    /**
     * Get currency code from marketplace
     * AliExpress → USD
     * Alibaba1688 → CNY
     *
     * @param marketplace The marketplace platform
     * @return Currency code (USD or CNY)
     */
    public static String getCurrencyFromMarketplace(Marketplace marketplace) {
        if (marketplace == null) {
            return "USD";
        }

        return switch (marketplace) {
            case ALIEXPRESS -> "USD";
            case ALIBABA1688 -> "CNY";
            default -> "USD";
        };
    }
}
