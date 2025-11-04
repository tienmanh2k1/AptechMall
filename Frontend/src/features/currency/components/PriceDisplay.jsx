import React from 'react';
import { useCurrency } from '../context/CurrencyContext';

const PriceDisplay = ({
  price,
  currency,
  showBoth = true,
  size = 'text-base',
  className = ''
}) => {
  const {
    formatPrice: formatPriceUtil,
    loading
  } = useCurrency();

  if (!price || !currency) {
    return <span className={`${size} text-gray-500 ${className}`}>
      N/A
    </span>;
  }

  const formatted = formatPriceUtil(price, currency);

  // Show loading indicator while fetching exchange rates
  if (loading) {
    return (
      <div className={className}>
        <div className={`${size} font-semibold text-red-600`}>
          {formatted.original}
        </div>
        <div className="text-xs text-gray-400">Đang tải tỷ giá...</div>
      </div>
    );
  }

  // Always show both prices: VND (main) and original currency (secondary)
  return (
    <div className={className}>
      {/* Main price: VND (if available) or original */}
      <div className={`${size} font-semibold text-red-600`}>
        {formatted.vnd || formatted.original}
      </div>

      {/* Secondary price: Original currency (if VND is available) */}
      {formatted.vnd && (
        <div className="text-xs text-gray-500 mt-0.5">
          {formatted.original}
        </div>
      )}
    </div>
  );
};

export default PriceDisplay;
