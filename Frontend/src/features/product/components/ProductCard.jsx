// src/features/product/components/ProductCard.jsx
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Star, ShoppingCart, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { getCurrencySymbol } from '../../../shared/utils/formatters';
import PriceDisplay from '../../currency/components/PriceDisplay';
import { useAuth } from '../../auth/context/AuthContext';
import { useCart } from '../../cart/context/CartContext';
import { addToCart } from '../../cart/services/cartApi';

const ProductCard = ({ product }) => {
  const {
    itemId,
    id,  // Preferred field from backend
    title = 'No title',
    imageUrl = '/placeholder.png',
    currentPrice = '0',
    originalPrice,
    discount,
    rating = 0,
    reviewCount = 0,  // Number of reviews
    totalSales = 0,
    videoUrl,
    platform,  // Platform field from backend (required)
    currency: currencyFromBackend,  // Currency code (USD, CNY, etc.)
    currencySign  // Currency symbol from backend ($, ¥, etc.)
  } = product;

  // Hooks
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { refreshCart } = useCart();
  const [adding, setAdding] = useState(false);

  // Use id if available, otherwise fallback to itemId
  const productId = id || itemId;

  // Map currencySign to currency code if currency not provided
  const mapCurrencySignToCode = (sign) => {
    const mapping = {
      '$': 'USD',
      '¥': 'CNY',
      '元': 'CNY',  // Chinese Yuan symbol
      '€': 'EUR',
      '£': 'GBP'
    };
    return mapping[sign] || 'USD';
  };

  // Determine currency: use explicit currency from backend, or map from currencySign, or default based on platform
  const currency = currencyFromBackend ||
                   (currencySign ? mapCurrencySignToCode(currencySign) : null) ||
                   (platform === '1688' ? 'CNY' : 'USD');

  // Use currencySign from backend if available, otherwise get from currency code
  const currencySymbol = currencySign || getCurrencySymbol(currency);

  // Build link using canonical path: /{platform}/products/{id}
  // Do NOT render link if platform or id is missing
  const hasValidData = platform && productId;
  const productLink = hasValidData ? `/${platform}/products/${productId}` : null;

  // Handle Add to Cart
  const handleAddToCart = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    // Check authentication
    if (!isAuthenticated()) {
      toast.warning('Vui lòng đăng nhập để thêm vào giỏ hàng');
      navigate('/login', { state: { from: window.location.pathname } });
      return;
    }

    setAdding(true);
    try {
      await addToCart({
        id: productId,          // Backend expects "id"
        title: title,           // Will be transformed to "productName"
        price: parseFloat(currentPrice),
        quantity: 1,
        image: imageUrl,        // Backend expects "image" (not "imageUrl")
        platform: platform      // Will be transformed to uppercase marketplace
      });

      toast.success('✅ Đã thêm vào giỏ hàng!');
      refreshCart(); // Update cart badge
    } catch (error) {
      console.error('Error adding to cart:', error);
      toast.error(error.response?.data?.message || 'Không thể thêm vào giỏ hàng');
    } finally {
      setAdding(false);
    }
  };

  // If no valid data, show validation hint (non-clickable card)
  if (!hasValidData) {
    return (
      <div className="bg-white rounded-lg shadow-sm overflow-hidden border-2 border-red-200 opacity-75">
        {/* Image */}
        <div className="aspect-square overflow-hidden bg-gray-100 relative">
          <img
            src={imageUrl}
            alt={title}
            className="w-full h-full object-cover"
            loading="lazy"
          />

          {/* Video Badge */}
          {videoUrl && (
            <div className="absolute top-2 right-2 bg-red-600 text-white text-xs px-2 py-1 rounded">
              📹 Video
            </div>
          )}

          {/* Discount Badge */}
          {discount && (
            <div className="absolute top-2 left-2 bg-yellow-400 text-gray-900 text-xs font-bold px-2 py-1 rounded">
              -{discount}%
            </div>
          )}
        </div>

        {/* Content */}
        <div className="p-4">
          {/* Validation Warning */}
          <div className="mb-3 flex items-start gap-2 bg-red-50 border border-red-200 rounded p-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
            <p className="text-xs text-red-700">
              Missing {!platform && 'platform'}{!platform && !productId && ' and '}{!productId && 'product ID'}
            </p>
          </div>

          {/* Title */}
          <h3 className="text-sm text-gray-900 font-medium line-clamp-2 mb-2 min-h-[40px]">
            {title}
          </h3>

          {/* Price */}
          <div className="mb-2">
            <PriceDisplay
              price={parseFloat(currentPrice)}
              currency={currency}
              showBoth={true}
              size="text-xl"
            />
            {originalPrice && parseFloat(originalPrice) > parseFloat(currentPrice) && (
              <div className="text-sm text-gray-400 line-through mt-1">
                {currencySymbol}{originalPrice}
              </div>
            )}
          </div>

          {/* Rating & Sales */}
          <div className="flex items-center justify-between text-xs text-gray-600 mb-3">
            <div className="flex items-center gap-1">
              <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
              <span>{rating ? rating.toFixed(1) : 'N/A'}</span>
              {reviewCount > 0 && (
                <span className="text-gray-400">({reviewCount})</span>
              )}
            </div>
            {totalSales > 0 && (
              <span>{totalSales.toLocaleString()} sold</span>
            )}
          </div>

          {/* Disabled Button */}
          <button
            disabled
            className="w-full py-2 bg-gray-300 text-gray-500 rounded-lg font-medium cursor-not-allowed flex items-center justify-center gap-2"
          >
            <ShoppingCart className="w-4 h-4" />
            Unavailable
          </button>
        </div>
      </div>
    );
  }

  // Normal card with valid data
  return (
    <Link
      to={productLink}
      className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow overflow-hidden group"
      onClick={() => {
        // Track click analytics
        console.log('Product clicked:', {
          platform,
          id: productId,
          listContext: 'search'
        });
      }}
    >
      {/* Image */}
      <div className="aspect-square overflow-hidden bg-gray-100 relative">
        <img
          src={imageUrl}
          alt={title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />

        {/* Video Badge */}
        {videoUrl && (
          <div className="absolute top-2 right-2 bg-red-600 text-white text-xs px-2 py-1 rounded">
            📹 Video
          </div>
        )}

        {/* Discount Badge */}
        {discount && (
          <div className="absolute top-2 left-2 bg-yellow-400 text-gray-900 text-xs font-bold px-2 py-1 rounded">
            -{discount}%
          </div>
        )}
      </div>

      {/* Content */}
      <div className="p-4">
        {/* Title */}
        <h3 className="text-sm text-gray-900 font-medium line-clamp-2 mb-2 min-h-[40px]">
          {title}
        </h3>

        {/* Price */}
        <div className="mb-2">
          <PriceDisplay
            price={parseFloat(currentPrice)}
            currency={currency}
            showBoth={true}
            size="text-xl"
          />
          {originalPrice && parseFloat(originalPrice) > parseFloat(currentPrice) && (
            <div className="text-sm text-gray-400 line-through mt-1">
              {currencySymbol}{originalPrice}
            </div>
          )}
        </div>

        {/* Rating & Sales */}
        <div className="flex items-center justify-between text-xs text-gray-600 mb-3">
          <div className="flex items-center gap-1">
            <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
            <span>{rating ? rating.toFixed(1) : 'N/A'}</span>
            {reviewCount > 0 && (
              <span className="text-gray-400">({reviewCount})</span>
            )}
          </div>
          {totalSales > 0 && (
            <span>{totalSales.toLocaleString()} sold</span>
          )}
        </div>

        {/* Add to Cart Button */}
        <button
          onClick={handleAddToCart}
          disabled={adding}
          className="w-full py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors flex items-center justify-center gap-2 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          <ShoppingCart className="w-4 h-4" />
          {adding ? 'Đang thêm...' : 'Thêm vào giỏ'}
        </button>
      </div>
    </Link>
  );
};

export default ProductCard;
