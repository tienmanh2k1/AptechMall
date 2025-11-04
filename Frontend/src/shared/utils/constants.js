export const APP_NAME = 'PandaMall';
export const API_TIMEOUT = 10000;

export const ROUTES = {
  HOME: '/',
  PRODUCT_DETAIL: '/product/:id',
  CART: '/cart',
  CHECKOUT: '/checkout',
  ORDERS: '/orders',
  LOGIN: '/login',
  REGISTER: '/register',
};

// Marketplace configuration
export const MARKETPLACES = {
  ALIEXPRESS: 'aliexpress',
  MALL_1688: '1688',
};

export const MARKETPLACE_CONFIG = {
  [MARKETPLACES.ALIEXPRESS]: {
    label: 'AliExpress',
    color: 'bg-red-600',
    textColor: 'text-red-600',
  },
  [MARKETPLACES.MALL_1688]: {
    label: '1688',
    color: 'bg-blue-600',
    textColor: 'text-blue-600',
  },
};

export const DEFAULT_MARKETPLACE = MARKETPLACES.ALIEXPRESS;

// Sort options mapping (frontend string to backend integer)
// Backend sortOptions: ["Default", "PriceAsc", "PriceDesc", "Sales"]
export const SORT_OPTIONS = {
  'default': 0,        // Default/Relevance
  'price-asc': 1,      // PriceAsc - Price: Low to High
  'price-desc': 2,     // PriceDesc - Price: High to Low
  'sales': 3,          // Sales - Best Selling
};

// Reverse mapping for display
export const SORT_LABELS = {
  'default': 'Most Relevant',
  'price-asc': 'Price: Low to High',
  'price-desc': 'Price: High to Low',
  'sales': 'Best Selling',
};