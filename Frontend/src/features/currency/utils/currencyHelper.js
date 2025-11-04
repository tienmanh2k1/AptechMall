/**
 * Get currency from item data
 * Priority:
 *   1. Explicit currency field (from backend)
 *   2. Infer from marketplace
 *   3. Default to USD
 */
export const getItemCurrency = (item) => {
  // Priority 1: Backend sent currency
  if (item.currency) {
    return item.currency.toUpperCase();
  }

  // Priority 2: Infer from marketplace
  if (item.marketplace || item.platform) {
    const marketplace = (item.marketplace || item.platform).toLowerCase();

    const currencyMap = {
      'aliexpress': 'USD',
      'alibaba1688': 'CNY',
      '1688': 'CNY'
    };

    return currencyMap[marketplace] || 'USD';
  }

  // Priority 3: Default
  return 'USD';
};
