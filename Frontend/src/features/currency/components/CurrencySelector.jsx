import React from 'react';
import { useCurrency } from '../context/CurrencyContext';
import { DollarSign, RefreshCw } from 'lucide-react';

const CurrencySelector = () => {
  const {
    displayCurrency,
    toggleCurrency,
    exchangeRates,
    loading
  } = useCurrency();

  // Show loading state instead of hiding completely
  if (loading || !exchangeRates) {
    return (
      <div className="flex items-center gap-2 px-3 py-2 text-sm font-medium
        text-gray-400 bg-gray-100 border border-gray-300 rounded-lg cursor-not-allowed">
        <DollarSign className="w-4 h-4" />
        <span>VND đ</span>
        <RefreshCw className="w-3 h-3 animate-spin" />
      </div>
    );
  }

  const getCurrencyLabel = () => {
    return displayCurrency === 'VND' ? 'VND đ' : 'USD/CNY';
  };

  const getTooltip = () => {
    const usdRate = exchangeRates.USD?.rateToVnd;
    const cnyRate = exchangeRates.CNY?.rateToVnd;
    const updatedAt = exchangeRates.USD?.updatedAt;

    if (!usdRate) return '';

    const date = new Date(updatedAt);
    const formattedDate = date.toLocaleDateString('vi-VN');

    return `1 USD = ${usdRate.toLocaleString('vi-VN')} đ
1 CNY = ${cnyRate.toLocaleString('vi-VN')} đ

Updated: ${formattedDate}`;
  };

  return (
    <button
      onClick={toggleCurrency}
      title={getTooltip()}
      className="flex items-center gap-2 px-3 py-2 text-sm font-medium
        text-gray-700 bg-white border border-gray-300 rounded-lg
        hover:bg-gray-50 transition-colors"
    >
      <DollarSign className="w-4 h-4" />
      <span>{getCurrencyLabel()}</span>
      <RefreshCw className="w-3 h-3 text-gray-400" />
    </button>
  );
};

export default CurrencySelector;
