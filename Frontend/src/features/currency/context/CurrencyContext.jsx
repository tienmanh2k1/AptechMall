import React, { createContext, useContext, useState, useEffect } from 'react';
import {
  getAllExchangeRates,
  convertToVND,
  formatCurrency
} from '../services/currencyApi';

const CurrencyContext = createContext();

export const useCurrency = () => {
  const context = useContext(CurrencyContext);
  if (!context) {
    throw new Error('useCurrency must be used within CurrencyProvider');
  }
  return context;
};

export const CurrencyProvider = ({ children }) => {
  const [exchangeRates, setExchangeRates] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchExchangeRates();

    // Refresh every hour
    const interval = setInterval(fetchExchangeRates, 3600000);
    return () => clearInterval(interval);
  }, []);

  const fetchExchangeRates = async () => {
    try {
      setLoading(true);
      setError(null);
      const rates = await getAllExchangeRates();
      setExchangeRates(rates);
      console.log('✅ Exchange rates loaded:', rates);
    } catch (err) {
      console.error('❌ Failed to load exchange rates:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const toVND = (amount, currency) => {
    if (!exchangeRates) return null;
    return convertToVND(amount, currency, exchangeRates);
  };

  const formatPrice = (amount, originalCurrency) => {
    const originalFormatted = formatCurrency(amount, originalCurrency);
    const vndAmount = toVND(amount, originalCurrency);
    const vndFormatted = vndAmount
      ? formatCurrency(vndAmount, 'VND')
      : null;

    return {
      original: originalFormatted,
      originalAmount: amount,
      originalCurrency,
      vnd: vndFormatted,
      vndAmount
    };
  };

  const value = {
    exchangeRates,
    loading,
    error,
    toVND,
    formatPrice,
    refreshRates: fetchExchangeRates
  };

  return (
    <CurrencyContext.Provider value={value}>
      {children}
    </CurrencyContext.Provider>
  );
};
