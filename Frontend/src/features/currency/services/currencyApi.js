import api from '../../../shared/services/api';

/**
 * Normalize currency symbol/code to ISO code
 */
const normalizeCurrency = (currency) => {
  if (!currency) return 'USD';

  // If already ISO code, return as-is
  if (['USD', 'CNY', 'VND', 'EUR', 'GBP'].includes(currency.toUpperCase())) {
    return currency.toUpperCase();
  }

  // Map symbols to ISO codes
  const symbolMap = {
    '$': 'USD',
    '¥': 'CNY',
    '元': 'CNY',  // Chinese Yuan symbol
    '₫': 'VND',
    'đ': 'VND',   // Vietnamese dong (common usage)
    '€': 'EUR',
    '£': 'GBP'
  };

  return symbolMap[currency] || 'USD';
};

/**
 * Get all exchange rates
 */
export const getAllExchangeRates = async () => {
  const response = await api.get('/exchange-rates');
  return response.data;
};

/**
 * Get specific currency rate
 */
export const getExchangeRate = async (currency) => {
  const normalizedCurrency = normalizeCurrency(currency);
  const response = await api.get(`/exchange-rates/${normalizedCurrency}`);
  return response.data;
};

/**
 * Convert amount to VND
 */
export const convertToVND = (amount, currency, rates) => {
  const normalizedCurrency = normalizeCurrency(currency);

  if (!rates || !rates[normalizedCurrency]) {
    console.warn(`No exchange rate found for ${currency} (normalized: ${normalizedCurrency})`);
    return null;
  }

  const rate = rates[normalizedCurrency].rateToVnd;
  return amount * rate;
};

/**
 * Format price with currency
 */
export const formatCurrency = (amount, currency) => {
  const normalizedCurrency = normalizeCurrency(currency);

  const formats = {
    USD: { symbol: '$', position: 'before', decimals: 2 },
    CNY: { symbol: '元', position: 'before', decimals: 2 },
    VND: { symbol: 'đ', position: 'after', decimals: 0 }  // Use Vietnamese 'd' for better font support
  };

  const format = formats[normalizedCurrency] || formats.USD;
  const formatted = new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: format.decimals,
    maximumFractionDigits: format.decimals
  }).format(amount);

  return format.position === 'before'
    ? `${format.symbol}${formatted}`
    : `${formatted} ${format.symbol}`;
};
