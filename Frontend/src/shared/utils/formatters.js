/**
 * Get currency symbol for a given currency code
 * @param {string} currency - ISO currency code (USD, CNY, etc.)
 * @returns {string} Currency symbol
 */
export const getCurrencySymbol = (currency = 'USD') => {
  const currencySymbols = {
    'USD': '$',
    'CNY': '¥',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'KRW': '₩',
    'RUB': '₽',
    'INR': '₹',
    'BRL': 'R$',
    'CAD': 'CA$',
    'AUD': 'A$'
  };

  return currencySymbols[currency] || currency + ' ';
};

/**
 * Format price with proper currency symbol
 * @param {number|string} price - Price value or range
 * @param {string} currency - ISO currency code
 * @returns {string} Formatted price string
 */
export const formatPrice = (price, currency = 'USD') => {
  const symbol = getCurrencySymbol(currency);

  if (typeof price === 'string' && price.includes('-')) {
    const [min, max] = price.split('-').map(p => parseFloat(p.trim()));
    return `${symbol}${min.toFixed(2)} - ${symbol}${max.toFixed(2)}`;
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
  }).format(price);
};

export const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

export const truncateText = (text, maxLength) => {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};
