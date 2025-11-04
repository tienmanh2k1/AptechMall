/**
 * Translation Cache Service
 * Stores translated product data in localStorage to avoid redundant API calls
 *
 * Cache structure:
 * {
 *   productId: "ae_1005005244562338",
 *   platform: "aliexpress",
 *   translatedAt: 1704067200000,
 *   expiresAt: 1704672000000,
 *   data: { ... translated product data ... }
 * }
 */

const CACHE_PREFIX = 'translation_';
const CACHE_TTL_DAYS = 7; // Cache for 7 days

/**
 * Generate cache key for a product
 *
 * @param {string} platform - Platform name ('aliexpress', '1688')
 * @param {string} productId - Product ID
 * @returns {string} Cache key
 */
const getCacheKey = (platform, productId) => {
  return `${CACHE_PREFIX}${platform}_${productId}`;
};

/**
 * Get cached translation for a product
 *
 * @param {string} platform - Platform name
 * @param {string} productId - Product ID
 * @returns {Object|null} Cached translation or null if not found/expired
 */
export const getCachedTranslation = (platform, productId) => {
  try {
    const cacheKey = getCacheKey(platform, productId);
    const cachedData = localStorage.getItem(cacheKey);

    if (!cachedData) {
      console.log('[Translation Cache] Miss:', cacheKey);
      return null;
    }

    const parsed = JSON.parse(cachedData);
    const now = Date.now();

    // Check if cache expired
    if (parsed.expiresAt && parsed.expiresAt < now) {
      console.log('[Translation Cache] Expired:', cacheKey);
      localStorage.removeItem(cacheKey);
      return null;
    }

    // console.log('[Translation Cache] Hit:', cacheKey); // Disabled to avoid log spam
    return parsed.data;

  } catch (error) {
    console.error('[Translation Cache] Error reading cache:', error);
    return null;
  }
};

/**
 * Save translated product data to cache
 *
 * @param {string} platform - Platform name
 * @param {string} productId - Product ID
 * @param {Object} translatedData - Translated product data
 * @returns {boolean} True if saved successfully
 */
export const saveCachedTranslation = (platform, productId, translatedData) => {
  try {
    const cacheKey = getCacheKey(platform, productId);
    const now = Date.now();
    const expiresAt = now + (CACHE_TTL_DAYS * 24 * 60 * 60 * 1000);

    const cacheData = {
      productId: `${platform}_${productId}`,
      platform,
      translatedAt: now,
      expiresAt,
      data: translatedData
    };

    localStorage.setItem(cacheKey, JSON.stringify(cacheData));
    console.log('[Translation Cache] Saved:', cacheKey);
    return true;

  } catch (error) {
    console.error('[Translation Cache] Error saving cache:', error);
    return false;
  }
};

/**
 * Clear cached translation for a product
 *
 * @param {string} platform - Platform name
 * @param {string} productId - Product ID
 * @returns {boolean} True if cleared successfully
 */
export const clearCachedTranslation = (platform, productId) => {
  try {
    const cacheKey = getCacheKey(platform, productId);
    localStorage.removeItem(cacheKey);
    console.log('[Translation Cache] Cleared:', cacheKey);
    return true;
  } catch (error) {
    console.error('[Translation Cache] Error clearing cache:', error);
    return false;
  }
};

/**
 * Clear all translation caches
 *
 * @returns {number} Number of caches cleared
 */
export const clearAllTranslationCaches = () => {
  try {
    let count = 0;
    const keys = Object.keys(localStorage);

    for (const key of keys) {
      if (key.startsWith(CACHE_PREFIX)) {
        localStorage.removeItem(key);
        count++;
      }
    }

    console.log(`[Translation Cache] Cleared all caches (${count} items)`);
    return count;

  } catch (error) {
    console.error('[Translation Cache] Error clearing all caches:', error);
    return 0;
  }
};

/**
 * Get cache statistics
 *
 * @returns {Object} Cache stats { total, expired, valid }
 */
export const getCacheStats = () => {
  try {
    const keys = Object.keys(localStorage);
    const now = Date.now();
    let total = 0;
    let expired = 0;
    let valid = 0;

    for (const key of keys) {
      if (key.startsWith(CACHE_PREFIX)) {
        total++;
        try {
          const data = JSON.parse(localStorage.getItem(key));
          if (data.expiresAt < now) {
            expired++;
          } else {
            valid++;
          }
        } catch (e) {
          expired++;
        }
      }
    }

    return { total, expired, valid };

  } catch (error) {
    console.error('[Translation Cache] Error getting stats:', error);
    return { total: 0, expired: 0, valid: 0 };
  }
};
