import React from 'react';
import { getMarketplaceInfo, normalizeMarketplace } from '../../../shared/utils/marketplace';
import { formatPrice } from '../../../shared/utils/formatters';
import PriceDisplay from '../../currency/components/PriceDisplay';
import { getItemCurrency } from '../../currency/utils/currencyHelper';

// Helper to parse variant data
const parseVariantData = (selectedVariant) => {
  if (!selectedVariant) return null;

  try {
    // If it's already an object, return it
    if (typeof selectedVariant === 'object') {
      return selectedVariant;
    }

    // If it's a string, parse it
    return JSON.parse(selectedVariant);
  } catch (err) {
    console.error('Failed to parse variant data:', err);
    return null;
  }
};

const OrderItemsList = ({ items }) => {
  if (!items || items.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No items in this order
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {items.map((item, index) => {
        // Support both platform (string) and marketplace (enum) fields
        const platform = item.platform || item.marketplace;
        const normalizedPlatform = normalizeMarketplace(platform);
        const marketplaceInfo = getMarketplaceInfo(platform);
        const subtotal = item.price * item.quantity;
        const variantData = parseVariantData(item.selectedVariant);

        // Get currency using the currency helper
        const currency = getItemCurrency(item);

        return (
          <div
            key={item.id || index}
            className="flex gap-4 p-4 bg-gray-50 rounded-lg"
          >
            {/* Product Image */}
            <div className="flex-shrink-0">
              <img
                src={item.image || item.productImage || '/placeholder.png'}
                alt={item.title || item.productName}
                className="w-20 h-20 object-cover rounded"
                onError={(e) => {
                  console.warn('Failed to load image:', item.image || item.productImage);
                  e.target.src = '/placeholder.png';
                }}
              />
            </div>

            {/* Product Info */}
            <div className="flex-1 min-w-0">
              <h4 className="text-sm font-medium text-gray-900 line-clamp-2 mb-2">
                {item.title || item.productName || 'Unnamed Product'}
              </h4>

              {/* Marketplace Badge */}
              <div className="mb-2">
                <span
                  className={`inline-block px-2 py-1 text-xs font-medium rounded ${marketplaceInfo.colors.bg} ${marketplaceInfo.colors.text}`}
                >
                  {marketplaceInfo.name}
                </span>
              </div>

              {/* Variant Information (New Format from Backend) */}
              {(item.variantName || item.variantOptions) && (
                <div className="text-xs mb-2 space-y-1">
                  {/* Variant Name (e.g., "Size M - Red") */}
                  {item.variantName && (
                    <div className="text-gray-700">
                      <span className="font-medium">Variant:</span> {item.variantName}
                    </div>
                  )}

                  {/* Variant Options (e.g., "Size: M, Color: Red") */}
                  {item.variantOptions && (
                    <div className="text-gray-600 text-xs">
                      {item.variantOptions}
                    </div>
                  )}
                </div>
              )}

              {/* Legacy Variant Format (for old orders) - Fallback */}
              {!item.variantName && !item.variantOptions && variantData && (
                <div className="text-xs mb-2 space-y-1">
                  {/* Variant Price */}
                  {variantData.price && (
                    <div className="text-gray-600">
                      <span className="font-medium">Variant Price:</span> {formatPrice(parseFloat(variantData.price), currency)}
                    </div>
                  )}

                  {/* Selected Options */}
                  {variantData.selectedOptions && Object.keys(variantData.selectedOptions).length > 0 && (
                    <div className="text-gray-600">
                      <span className="font-medium">Options:</span>
                      <div className="flex flex-wrap gap-1 mt-0.5">
                        {Object.entries(variantData.selectedOptions).map(([key, value]) => (
                          <span key={key} className="inline-block px-1.5 py-0.5 bg-gray-200 text-gray-700 rounded text-xs">
                            {value}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Price and Quantity */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <PriceDisplay
                    price={item.price}
                    currency={currency}
                    showBoth={false}
                    size="text-sm"
                  />
                  <span className="text-sm text-gray-600">× {item.quantity}</span>
                </div>
                <PriceDisplay
                  price={subtotal}
                  currency={currency}
                  showBoth={true}
                  size="text-sm"
                  className="font-semibold"
                />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default OrderItemsList;
