/**
 * Product Translator Utility
 * Handles translation of all product-related content for e-commerce
 */

import { translateBatch, getSourceLanguage } from '../services/translationApi';

/**
 * Extract all translatable text from product data
 *
 * @param {Object} product - Product data
 * @returns {Object} { texts: string[], paths: string[] }
 */
const extractTranslatableTexts = (product) => {
  const texts = [];
  const paths = [];

  // Helper to add text with path
  const addText = (text, path) => {
    if (text && typeof text === 'string' && text.trim() !== '') {
      texts.push(text);
      paths.push(path);
    }
  };

  // 1. Title (only translate title to avoid rate limit)
  addText(product.title, 'title');

  // NOTE: Chỉ dịch title để tránh rate limit 429
  // Các field khác bị comment out để giảm API calls từ 20-30 xuống còn 1

  // // 2. Description
  // addText(product.description, 'description');

  // // 3. Attributes
  // if (product.attributes && Array.isArray(product.attributes)) {
  //   product.attributes.forEach((attr, index) => {
  //     addText(attr.name, `attributes.${index}.name`);
  //     addText(attr.value, `attributes.${index}.value`);
  //   });
  // }

  // // 4. Variants/SKU properties
  // if (product.variants && Array.isArray(product.variants)) {
  //   product.variants.forEach((variant, index) => {
  //     addText(variant.name, `variants.${index}.name`);
  //     if (variant.value) {
  //       addText(variant.value, `variants.${index}.value`);
  //     }
  //   });
  // }

  // // 5. Shop/Seller info
  // if (product.shop) {
  //   addText(product.shop.name, 'shop.name');
  //   addText(product.shop.description, 'shop.description');
  // }

  // // 6. Shipping info
  // if (product.shipping) {
  //   addText(product.shipping.method, 'shipping.method');
  //   addText(product.shipping.description, 'shipping.description');
  // }

  // // 7. Service guarantees
  // if (product.service && Array.isArray(product.service)) {
  //   product.service.forEach((service, index) => {
  //     addText(service, `service.${index}`);
  //   });
  // }

  // // 8. Legacy fields (for backward compatibility)
  // if (product.result) {
  //   // Item details
  //   if (product.result.item) {
  //     addText(product.result.item.title, 'result.item.title');
  //     addText(product.result.item.description, 'result.item.description');

  //     // Item properties
  //     if (product.result.item.properties && Array.isArray(product.result.item.properties)) {
  //       product.result.item.properties.forEach((prop, index) => {
  //         addText(prop.name, `result.item.properties.${index}.name`);
  //         addText(prop.value, `result.item.properties.${index}.value`);
  //       });
  //     }
  //   }

  //   // Delivery info
  //   if (product.result.delivery) {
  //     addText(product.result.delivery.method, 'result.delivery.method');
  //     addText(product.result.delivery.description, 'result.delivery.description');
  //   }

  //   // Service guarantees
  //   if (product.result.service && Array.isArray(product.result.service)) {
  //     product.result.service.forEach((service, index) => {
  //       addText(service, `result.service.${index}`);
  //     });
  //   }

  //   // Seller info
  //   if (product.result.seller) {
  //     addText(product.result.seller.name, 'result.seller.name');
  //     addText(product.result.seller.description, 'result.seller.description');
  //   }
  // }

  return { texts, paths };
};

/**
 * Apply translated texts back to product object
 *
 * @param {Object} product - Original product data
 * @param {string[]} translatedTexts - Array of translated texts
 * @param {string[]} paths - Array of paths matching translatedTexts
 * @returns {Object} Product with translated content
 */
const applyTranslations = (product, translatedTexts, paths) => {
  // Deep clone product to avoid mutation
  const translated = JSON.parse(JSON.stringify(product));

  paths.forEach((path, index) => {
    const translatedText = translatedTexts[index];
    if (!translatedText) return;

    // Split path and navigate to set value
    const pathParts = path.split('.');
    let obj = translated;

    for (let i = 0; i < pathParts.length - 1; i++) {
      const part = pathParts[i];
      if (!obj[part]) {
        obj[part] = {};
      }
      obj = obj[part];
    }

    // Set the translated value
    const lastPart = pathParts[pathParts.length - 1];
    obj[lastPart] = translatedText;
  });

  return translated;
};

/**
 * Translate entire product data
 *
 * @param {Object} product - Product data
 * @param {string} platform - Platform name ('aliexpress', '1688')
 * @param {Object} options - Options { delayMs: number }
 * @returns {Promise<Object>} Translated product data
 */
export const translateProduct = async (product, platform, options = {}) => {
  const { delayMs = 500 } = options;

  try {
    console.log('[Product Translator] Starting translation for platform:', platform);

    // Extract all texts
    const { texts, paths } = extractTranslatableTexts(product);
    console.log(`[Product Translator] Extracted ${texts.length} texts to translate`);

    if (texts.length === 0) {
      console.log('[Product Translator] No texts to translate');
      return product;
    }

    // Get source language
    const sourceLang = getSourceLanguage(platform);
    console.log(`[Product Translator] Source language: ${sourceLang} → vi`);

    // Translate all texts
    const translatedTexts = await translateBatch(texts, sourceLang, 'vi', delayMs);
    console.log(`[Product Translator] Translated ${translatedTexts.length} texts`);

    // Apply translations
    const translatedProduct = applyTranslations(product, translatedTexts, paths);
    console.log('[Product Translator] Translation complete');

    return translatedProduct;

  } catch (error) {
    console.error('[Product Translator] Error:', error);
    // Return original product if translation fails
    return product;
  }
};

/**
 * Get estimated translation time
 *
 * @param {Object} product - Product data
 * @param {number} delayMs - Delay between requests (default: 500ms)
 * @returns {number} Estimated time in milliseconds
 */
export const estimateTranslationTime = (product, delayMs = 500) => {
  const { texts } = extractTranslatableTexts(product);
  const requestTime = 1000; // Average 1 second per request
  return texts.length * (requestTime + delayMs);
};

/**
 * Get translation progress info
 *
 * @param {number} current - Current text index
 * @param {number} total - Total texts to translate
 * @returns {Object} { percentage, message }
 */
export const getTranslationProgress = (current, total) => {
  const percentage = Math.round((current / total) * 100);
  const message = `Đang dịch... ${current}/${total} (${percentage}%)`;

  return { percentage, message };
};
