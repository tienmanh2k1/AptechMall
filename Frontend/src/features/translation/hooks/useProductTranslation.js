/**
 * useProductTranslation Hook
 * Automatically translates product content to Vietnamese with caching
 *
 * Usage:
 * const { translatedProduct, isTranslating, error, showOriginal, toggleLanguage } =
 *   useProductTranslation(product, platform);
 */

import { useState, useEffect, useRef, useMemo } from 'react';
import { translateProduct } from '../utils/productTranslator';
import { getCachedTranslation, saveCachedTranslation } from '../services/translationCache';
import { needsTranslation } from '../services/translationApi';

/**
 * Hook to auto-translate product data
 *
 * @param {Object} product - Product data from API
 * @param {string} platform - Platform name ('aliexpress', '1688')
 * @param {Object} options - Options { autoTranslate: boolean, delayMs: number }
 * @returns {Object} { translatedProduct, originalProduct, isTranslating, error, showOriginal, toggleLanguage, retryTranslation }
 */
export const useProductTranslation = (product, platform, options = {}) => {
  const {
    autoTranslate = true,  // Auto-translate when product loads
    delayMs = 500          // Delay between translation requests
  } = options;

  const [translatedProduct, setTranslatedProduct] = useState(null);
  const [originalProduct, setOriginalProduct] = useState(null);
  const [isTranslating, setIsTranslating] = useState(false);
  const [error, setError] = useState(null);
  const [showOriginal, setShowOriginal] = useState(false);

  // Track last translated product ID to prevent infinite loops
  const lastTranslatedIdRef = useRef(null);
  const isTranslatingRef = useRef(false);

  // Stable productId to prevent unnecessary re-renders
  const productId = useMemo(() => {
    return product?.id || product?.productId;
  }, [product?.id, product?.productId]);

  /**
   * Perform translation
   */
  const performTranslation = async (productData, platformName) => {
    if (!productData || !platformName) {
      return;
    }

    // Prevent multiple simultaneous translations
    if (isTranslatingRef.current) {
      return;
    }

    // Check if translation is needed for this platform
    if (!needsTranslation(platformName)) {
      console.log('[useProductTranslation] Platform does not need translation:', platformName);
      setTranslatedProduct(productData);
      setOriginalProduct(productData);
      isTranslatingRef.current = false;
      return;
    }

    isTranslatingRef.current = true;
    setIsTranslating(true);
    setError(null);

    try {
      const productId = productData.id || productData.productId;

      // Check cache first
      const cached = getCachedTranslation(platformName, productId);
      if (cached) {
        // console.log('[useProductTranslation] Using cached translation'); // Disabled to avoid log spam
        setTranslatedProduct(cached);
        setOriginalProduct(productData);
        isTranslatingRef.current = false;
        setIsTranslating(false);
        return;
      }

      // Translate product
      console.log('[useProductTranslation] Starting translation...');
      const translated = await translateProduct(productData, platformName, { delayMs });

      // Save to cache
      saveCachedTranslation(platformName, productId, translated);

      setTranslatedProduct(translated);
      setOriginalProduct(productData);
      console.log('[useProductTranslation] Translation complete');

    } catch (err) {
      console.error('[useProductTranslation] Translation failed:', err);
      setError(err.message || 'Translation failed');
      // Fallback to original product
      setTranslatedProduct(productData);
      setOriginalProduct(productData);
    } finally {
      isTranslatingRef.current = false;
      setIsTranslating(false);
    }
  };

  /**
   * Effect: Auto-translate when product changes
   */
  useEffect(() => {
    if (!product || !productId) return;

    const cacheKey = `${platform}_${productId}`;

    // Skip if already translated this product (prevent infinite loop)
    if (lastTranslatedIdRef.current === cacheKey) {
      return;
    }

    if (platform && autoTranslate) {
      lastTranslatedIdRef.current = cacheKey;
      performTranslation(product, platform);
    } else {
      // No auto-translate, just set original
      setOriginalProduct(product);
      setTranslatedProduct(product);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productId, platform]); // Only re-run when productId or platform changes

  /**
   * Toggle between original and translated version
   */
  const toggleLanguage = () => {
    setShowOriginal(prev => !prev);
  };

  /**
   * Retry translation manually
   */
  const retryTranslation = () => {
    if (originalProduct && platform) {
      performTranslation(originalProduct, platform);
    }
  };

  /**
   * Get current display product (original or translated)
   */
  const displayProduct = showOriginal ? originalProduct : translatedProduct;

  return {
    translatedProduct,
    originalProduct,
    displayProduct,      // Current product to display (respects showOriginal flag)
    isTranslating,
    error,
    showOriginal,
    toggleLanguage,
    retryTranslation,
    hasTranslation: !!translatedProduct && translatedProduct !== originalProduct
  };
};

export default useProductTranslation;
