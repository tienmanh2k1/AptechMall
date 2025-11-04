/**
 * Marketplace Utilities
 * Handles canonical marketplace enum, globalId parsing, and validation
 */

// Canonical marketplace enum
export const MARKETPLACE = {
  ALIEXPRESS: 'aliexpress',
  ALIBABA_1688: '1688'
};

// Prefix mapping for namespaced globalId format
// Includes aliases: ali|ae → aliexpress, a1688 → 1688
export const MARKETPLACE_PREFIX = {
  ae: MARKETPLACE.ALIEXPRESS,
  ali: MARKETPLACE.ALIEXPRESS,  // Alias for AliExpress
  a1688: MARKETPLACE.ALIBABA_1688
};

// Reverse mapping: marketplace → prefix (canonical only)
export const PREFIX_BY_MARKETPLACE = {
  [MARKETPLACE.ALIEXPRESS]: 'ae',
  [MARKETPLACE.ALIBABA_1688]: 'a1688'
};

// Marketplace aliases for backend legacy route support
export const MARKETPLACE_ALIASES = {
  'ali': MARKETPLACE.ALIEXPRESS,
  'ae': MARKETPLACE.ALIEXPRESS,
  'aliexpress': MARKETPLACE.ALIEXPRESS,
  'a1688': MARKETPLACE.ALIBABA_1688,
  '1688': MARKETPLACE.ALIBABA_1688
};

// Display names for UI
export const MARKETPLACE_DISPLAY_NAME = {
  [MARKETPLACE.ALIEXPRESS]: 'AliExpress',
  [MARKETPLACE.ALIBABA_1688]: '1688'
};

// Brand colors for badges
export const MARKETPLACE_COLORS = {
  [MARKETPLACE.ALIEXPRESS]: { bg: 'bg-red-500', text: 'text-white' },
  [MARKETPLACE.ALIBABA_1688]: { bg: 'bg-blue-500', text: 'text-white' }
};

/**
 * Normalize a marketplace alias to canonical marketplace enum
 * @param {string} alias - Marketplace alias or canonical name
 * @returns {string|null} Canonical marketplace enum or null if invalid
 */
export function normalizeMarketplaceAlias(alias) {
  if (!alias) return null;
  return MARKETPLACE_ALIASES[alias.toLowerCase()] || null;
}

/**
 * Parse a namespaced globalId into its components
 * Supports both canonical prefixes (ae, a1688) and aliases (ali)
 * @param {string} globalId - Format: "prefix:providerId" (e.g., "ae:1005005244562338" or "ali:1005005244562338")
 * @returns {{ marketplace: string, productId: string, isValid: boolean, error?: string }}
 */
export function parseGlobalId(globalId) {
  if (!globalId || typeof globalId !== 'string') {
    return {
      marketplace: null,
      productId: null,
      isValid: false,
      error: 'VALIDATION_ERROR: globalId is required and must be a string'
    };
  }

  // Check if it contains the separator
  if (!globalId.includes(':')) {
    // Legacy format - no prefix
    return {
      marketplace: null,
      productId: globalId,
      isValid: false,
      isLegacy: true,
      error: 'LEGACY_FORMAT: No marketplace prefix found'
    };
  }

  const parts = globalId.split(':');

  // Validate format
  if (parts.length !== 2) {
    return {
      marketplace: null,
      productId: null,
      isValid: false,
      error: 'VALIDATION_ERROR: Invalid globalId format. Expected "prefix:productId"'
    };
  }

  const [prefix, productId] = parts;

  // Validate prefix (including aliases)
  const marketplace = MARKETPLACE_PREFIX[prefix.toLowerCase()];
  if (!marketplace) {
    return {
      marketplace: null,
      productId: null,
      isValid: false,
      error: `VALIDATION_ERROR: Unsupported marketplace prefix "${prefix}". Allowed: ${Object.keys(MARKETPLACE_PREFIX).join(', ')}`
    };
  }

  // Validate productId is not empty
  if (!productId || productId.trim() === '') {
    return {
      marketplace,
      productId: null,
      isValid: false,
      error: 'VALIDATION_ERROR: Product ID cannot be empty'
    };
  }

  return {
    marketplace,
    productId: productId.trim(),
    prefix: prefix.toLowerCase(), // Return normalized prefix
    isValid: true
  };
}

/**
 * Build a canonical globalId from marketplace and productId
 * @param {string} marketplace - One of MARKETPLACE enum values
 * @param {string} productId - The provider's product ID
 * @returns {string} Namespaced globalId (e.g., "ae:1005005244562338")
 */
export function buildGlobalId(marketplace, productId) {
  const prefix = PREFIX_BY_MARKETPLACE[marketplace];

  if (!prefix) {
    throw new Error(`Invalid marketplace: ${marketplace}. Must be one of: ${Object.keys(PREFIX_BY_MARKETPLACE).join(', ')}`);
  }

  if (!productId) {
    throw new Error('Product ID is required');
  }

  return `${prefix}:${productId}`;
}

/**
 * Validate a marketplace enum value
 * @param {string} marketplace - Marketplace to validate
 * @returns {boolean}
 */
export function isValidMarketplace(marketplace) {
  return Object.values(MARKETPLACE).includes(marketplace);
}

/**
 * Convert a legacy productId to canonical format (default to AliExpress)
 * @param {string} productId - Legacy product ID without marketplace prefix
 * @returns {string} Canonical globalId with default marketplace
 */
export function legacyToCanonical(productId) {
  // Default to AliExpress for backward compatibility during transition
  return buildGlobalId(MARKETPLACE.ALIEXPRESS, productId);
}

/**
 * Normalize backend marketplace enum to frontend value
 * Backend sends: ALIEXPRESS, ALIBABA1688
 * Frontend uses: aliexpress, 1688
 * @param {string} marketplace - Marketplace value from backend or URL
 * @returns {string} Normalized marketplace value
 */
export function normalizeMarketplace(marketplace) {
  if (!marketplace) return null;

  const normalized = marketplace.toLowerCase();

  // Map backend enum names to frontend values
  if (normalized === 'alibaba1688') return '1688';
  if (normalized === 'aliexpress') return 'aliexpress';

  // Already normalized or unknown
  return normalized;
}

/**
 * Get marketplace display information
 * @param {string} marketplace - Marketplace enum value
 * @returns {{ name: string, colors: { bg: string, text: string } }}
 */
export function getMarketplaceInfo(marketplace) {
  // Normalize first to handle backend enum names
  const normalized = normalizeMarketplace(marketplace);

  return {
    name: MARKETPLACE_DISPLAY_NAME[normalized] || 'Unknown',
    colors: MARKETPLACE_COLORS[normalized] || { bg: 'bg-gray-500', text: 'text-white' }
  };
}
