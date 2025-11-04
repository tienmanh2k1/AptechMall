import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Minus, Plus, Trash2 } from 'lucide-react';
import { getMarketplaceInfo, normalizeMarketplace } from '../../../shared/utils/marketplace';
import { formatPrice } from '../../../shared/utils/formatters';
import PriceDisplay from '../../currency/components/PriceDisplay';
import { getItemCurrency } from '../../currency/utils/currencyHelper';

const CartItem = ({ item, onUpdateQuantity, onRemove, loading, selected = false, onSelect }) => {
  // Support both platform and marketplace fields (backend sends marketplace in uppercase)
  const platform = item.platform || item.marketplace;
  const marketplaceInfo = getMarketplaceInfo(platform);

  // Build canonical product link: /{platform}/products/{productId}
  const normalizedPlatform = normalizeMarketplace(platform);
  const productId = item.productId;
  const hasValidProductData = normalizedPlatform && productId;
  const productLink = hasValidProductData ? `/${normalizedPlatform}/products/${productId}` : null;

  // Ensure price is a number
  const price = typeof item.price === 'number' ? item.price : parseFloat(item.price) || 0;
  const subtotal = price * item.quantity;
  const [note, setNote] = useState('');

  // Get currency using the currency helper
  const currency = getItemCurrency(item);

  // Get variant display from new backend format
  const variantDisplay = useMemo(() => {
    // Check for new format (variantName from backend)
    if (item.variantName) {
      return item.variantName;
    }

    // Fallback: Parse old selectedVariant format for backward compatibility
    if (item.selectedVariant) {
      try {
        const variantData = typeof item.selectedVariant === 'object'
          ? item.selectedVariant
          : JSON.parse(item.selectedVariant);

        if (variantData.selectedOptions && Object.keys(variantData.selectedOptions).length > 0) {
          return Object.values(variantData.selectedOptions).join(' + ');
        }
      } catch (err) {
        console.error('Failed to parse variant data:', err);
      }
    }

    return null;
  }, [item.variantName, item.selectedVariant]);

  // Get detailed variant options (e.g., "Size: M, Color: Red")
  const variantOptions = item.variantOptions || null;

  const handleDecrease = () => {
    if (item.quantity > 1) {
      onUpdateQuantity(item.id, item.quantity - 1);
    }
  };

  const handleIncrease = () => {
    onUpdateQuantity(item.id, item.quantity + 1);
  };

  // Get currency symbol
  const getCurrencySymbol = (currency) => {
    if (currency === 'CNY') return '¥';
    if (currency === 'USD') return '$';
    return currency;
  };

  return (
    <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
      {/* Item Row */}
      <div className="flex items-start gap-4 p-4">
        {/* Checkbox */}
        <div className="flex-shrink-0 pt-1">
          <input
            type="checkbox"
            checked={selected}
            onChange={(e) => onSelect?.(item.id, e.target.checked)}
            className="w-5 h-5 text-orange-500 border-gray-300 rounded focus:ring-orange-500 cursor-pointer"
          />
        </div>

        {/* Product Image */}
        <div className="flex-shrink-0">
          <img
            src={item.image || item.productImage || '/placeholder.png'}
            alt={item.title || item.productName || 'Product'}
            className="w-20 h-20 object-cover rounded border border-gray-200"
            onError={(e) => {
              console.warn('Failed to load image:', item.image || item.productImage);
              e.target.src = '/placeholder.png';
            }}
          />
        </div>

        {/* Product Info */}
        <div className="flex-1 min-w-0">
          {/* Marketplace Badge */}
          {marketplaceInfo && (
            <div className="mb-2">
              <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${marketplaceInfo.colors.bg} ${marketplaceInfo.colors.text}`}>
                {marketplaceInfo.name}
              </span>
            </div>
          )}

          {/* Title - Clickable link to product detail page */}
          {productLink ? (
            <Link
              to={productLink}
              className="text-sm font-normal text-gray-800 line-clamp-2 mb-2 leading-relaxed hover:text-red-600 transition-colors block"
            >
              {item.title || item.productName || 'Unnamed Product'}
            </Link>
          ) : (
            <h3 className="text-sm font-normal text-gray-800 line-clamp-2 mb-2 leading-relaxed">
              {item.title || item.productName || 'Unnamed Product'}
            </h3>
          )}

          {/* Variant Info - Shows variant name and options */}
          {variantDisplay && (
            <div className="text-xs text-gray-600 mb-3 space-y-1">
              <div className="font-medium">
                {variantDisplay}
              </div>
              {variantOptions && (
                <div className="text-gray-500">
                  {variantOptions}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Price */}
        <div className="flex-shrink-0 text-right" style={{ minWidth: '100px' }}>
          <PriceDisplay
            price={price}
            currency={currency}
            showBoth={true}
            size="text-base"
          />
        </div>

        {/* Quantity Controls */}
        <div className="flex-shrink-0">
          <div className="flex items-center border border-gray-300 rounded">
            <button
              onClick={handleDecrease}
              disabled={loading || item.quantity <= 1}
              className="w-8 h-8 flex items-center justify-center text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <Minus className="w-3.5 h-3.5" />
            </button>
            <input
              type="text"
              value={item.quantity}
              readOnly
              className="w-12 h-8 text-center text-sm border-x border-gray-300 focus:outline-none"
            />
            <button
              onClick={handleIncrease}
              disabled={loading}
              className="w-8 h-8 flex items-center justify-center text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Subtotal */}
        <div className="flex-shrink-0 text-right" style={{ minWidth: '100px' }}>
          <PriceDisplay
            price={subtotal}
            currency={currency}
            showBoth={true}
            size="text-base"
          />
        </div>

        {/* Delete Button */}
        <div className="flex-shrink-0">
          <button
            onClick={() => onRemove(item.id)}
            disabled={loading}
            className="p-2 text-gray-400 hover:text-red-500 transition-colors disabled:opacity-50"
            title="Xóa"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Note Field */}
      <div className="px-4 pb-4">
        <input
          type="text"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Ghi chú cho sản phẩm"
          className="w-full px-3 py-2 text-sm border border-gray-200 rounded focus:outline-none focus:border-orange-400 focus:ring-1 focus:ring-orange-400"
        />
      </div>
    </div>
  );
};

export default CartItem;
