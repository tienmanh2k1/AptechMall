/**
 * Translation API Service using RapidAPI Free Google Translator
 *
 * API: https://rapidapi.com/dickyagustin/api/free-google-translator
 * Translates product content from English/Chinese to Vietnamese
 */

import axios from 'axios';

const RAPIDAPI_KEY = '67e2ad2683msh7fa1cbe1ceab4bap18df02jsn668825d93d36';
const RAPIDAPI_HOST = 'free-google-translator.p.rapidapi.com';
const API_BASE_URL = 'https://free-google-translator.p.rapidapi.com/external-api/free-google-translator';

/**
 * Translate a single text using RapidAPI Google Translator with retry logic
 *
 * @param {string} text - Text to translate
 * @param {string} sourceLang - Source language code ('en', 'zh-CN', 'zh-TW')
 * @param {string} targetLang - Target language code (default: 'vi')
 * @param {number} retries - Number of retry attempts for 429 errors (default: 2)
 * @returns {Promise<string>} Translated text
 */
export const translateText = async (text, sourceLang = 'en', targetLang = 'vi', retries = 2) => {
  if (!text || text.trim() === '') {
    return text;
  }

  let lastError = null;

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      if (attempt > 0) {
        // Exponential backoff: 2s, 4s, 8s...
        const backoffDelay = Math.pow(2, attempt) * 1000;
        console.log(`[Translation] Retry ${attempt}/${retries} after ${backoffDelay}ms...`);
        await new Promise(resolve => setTimeout(resolve, backoffDelay));
      }

      console.log(`[Translation] Translating from ${sourceLang} to ${targetLang}:`, text.substring(0, 50) + '...');

      const response = await axios.post(
        API_BASE_URL,
        {
          translate: 'rapidapi' // Required body parameter
        },
        {
          params: {
            from: sourceLang,
            to: targetLang,
            query: text  // Axios automatically encodes params
          },
          headers: {
            'x-rapidapi-key': RAPIDAPI_KEY,
            'x-rapidapi-host': RAPIDAPI_HOST,
            'Content-Type': 'application/json'
          }
        }
      );

      // Response format: { translation: "translated text" } or similar
      const translatedText = response.data?.translation || response.data?.translatedText || text;

      console.log(`[Translation] Success:`, translatedText.substring(0, 50) + '...');
      return translatedText;

    } catch (error) {
      lastError = error;

      // Handle 429 (Rate Limit) - retry with backoff
      if (error.response?.status === 429) {
        console.warn(`[Translation] Rate limit (429) - attempt ${attempt + 1}/${retries + 1}`);

        if (attempt < retries) {
          continue; // Retry
        } else {
          console.error('[Translation] Rate limit exceeded after retries. Returning original text.');
        }
      } else {
        // Other errors - don't retry
        console.error('[Translation] Error:', error.response?.data || error.message);
        break;
      }
    }
  }

  // Return original text if all retries fail
  return text;
};

/**
 * Translate multiple texts in sequence (with delay to avoid rate limit)
 *
 * @param {string[]} texts - Array of texts to translate
 * @param {string} sourceLang - Source language code
 * @param {string} targetLang - Target language code (default: 'vi')
 * @param {number} delayMs - Delay between requests in milliseconds (default: 1500ms)
 * @returns {Promise<string[]>} Array of translated texts
 */
export const translateBatch = async (texts, sourceLang = 'en', targetLang = 'vi', delayMs = 1500) => {
  const results = [];

  for (let i = 0; i < texts.length; i++) {
    const text = texts[i];

    if (!text || text.trim() === '') {
      results.push(text);
      continue;
    }

    // Translate current text
    const translated = await translateText(text, sourceLang, targetLang);
    results.push(translated);

    // Add delay between requests to avoid rate limit (except for last item)
    if (i < texts.length - 1) {
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }

  return results;
};

/**
 * Detect source language from platform
 *
 * @param {string} platform - Platform name ('aliexpress', '1688')
 * @returns {string} Language code ('en', 'zh-CN')
 */
export const getSourceLanguage = (platform) => {
  const languageMap = {
    'aliexpress': 'en',      // AliExpress uses English
    '1688': 'zh-CN',         // 1688 uses Simplified Chinese
    'alibaba': 'zh-CN'       // Alibaba uses Simplified Chinese
  };

  return languageMap[platform.toLowerCase()] || 'en';
};

/**
 * Check if translation is needed
 *
 * @param {string} platform - Platform name
 * @returns {boolean} True if translation needed
 */
export const needsTranslation = (platform) => {
  const supportedPlatforms = ['aliexpress', '1688', 'alibaba'];
  return supportedPlatforms.includes(platform.toLowerCase());
};
