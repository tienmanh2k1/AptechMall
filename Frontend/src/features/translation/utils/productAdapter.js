/**
 * Product Adapter
 * Transforms backend product response to translation-friendly format
 */

/**
 * Transform backend product to flat structure for translation
 *
 * @param {Object} backendProduct - Product from backend API
 * @returns {Object} Flattened product for translation
 */
export const transformForTranslation = (backendProduct) => {
  if (!backendProduct || !backendProduct.Result || !backendProduct.Result.Item) {
    return backendProduct;
  }

  const item = backendProduct.Result.Item;
  const vendor = backendProduct.Result.Vendor;
  const rootPath = backendProduct.Result.RootPath;

  // Create flat structure for translation
  return {
    // Basic info
    id: item.Id, // Product ID for cache key
    title: item.Title,
    description: item.Description || '',

    // Attributes (product specs)
    attributes: item.Attributes?.map(attr => ({
      name: attr.PropertyName,
      value: attr.Value,
      _original: attr // Keep original for reference
    })) || [],

    // Variants (if configurator)
    variants: item.ConfiguredItems?.map(ci => ({
      name: ci.Title || '',
      _original: ci
    })) || [],

    // Shop/Seller
    shop: vendor ? {
      name: vendor.Name || vendor.ShopName || vendor.DisplayName || '',
      description: vendor.Description || '',
      _original: vendor
    } : null,

    // Category breadcrumbs
    breadcrumbs: rootPath?.Content?.map(c => ({
      title: c.Name || '',
      _original: c
    })) || [],

    // Keep original data
    _backendData: backendProduct
  };
};

/**
 * Apply translations back to backend product format
 *
 * @param {Object} translatedData - Translated flat product
 * @param {Object} originalBackendProduct - Original backend product
 * @returns {Object} Backend product with translated content
 */
export const applyTranslationsToBackend = (translatedData, originalBackendProduct) => {
  if (!translatedData || !originalBackendProduct) {
    return originalBackendProduct;
  }

  // Deep clone original
  const result = JSON.parse(JSON.stringify(originalBackendProduct));

  if (!result.Result || !result.Result.Item) {
    return result;
  }

  // Apply translations
  if (translatedData.title) {
    result.Result.Item.Title = translatedData.title;
  }

  if (translatedData.description) {
    result.Result.Item.Description = translatedData.description;
  }

  // Apply attribute translations
  if (translatedData.attributes && result.Result.Item.Attributes) {
    translatedData.attributes.forEach((translatedAttr, index) => {
      if (result.Result.Item.Attributes[index]) {
        if (translatedAttr.name) {
          result.Result.Item.Attributes[index].PropertyName = translatedAttr.name;
        }
        if (translatedAttr.value) {
          result.Result.Item.Attributes[index].Value = translatedAttr.value;
        }
      }
    });
  }

  // Apply vendor/shop translations
  if (translatedData.shop && result.Result.Vendor) {
    if (translatedData.shop.name) {
      result.Result.Vendor.Name = translatedData.shop.name;
      result.Result.Vendor.ShopName = translatedData.shop.name;
      result.Result.Vendor.DisplayName = translatedData.shop.name;
    }
    if (translatedData.shop.description) {
      result.Result.Vendor.Description = translatedData.shop.description;
    }
  }

  // Apply breadcrumb translations
  if (translatedData.breadcrumbs && result.Result.RootPath?.Content) {
    translatedData.breadcrumbs.forEach((translatedCrumb, index) => {
      if (result.Result.RootPath.Content[index] && translatedCrumb.title) {
        result.Result.RootPath.Content[index].Name = translatedCrumb.title;
      }
    });
  }

  return result;
};
