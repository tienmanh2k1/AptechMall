import React, { useState } from 'react';

const formatPrice = (price) => {
  if (!price) return 'N/A';
  const numPrice = parseFloat(price);
  if (isNaN(numPrice)) return price;
  return `$${numPrice.toFixed(2)}`;
};

const ProductSKU = ({ sku }) => {
  const [selectedVariant, setSelectedVariant] = useState(null);

  if (!sku || !sku.props || sku.props.length === 0) {
    return null;
  }

  const colorProp = sku.props.find(p => p.pid === 14);

  if (!colorProp || !colorProp.values) {
    return null;
  }

  const getVariantInfo = (vid) => {
    if (!sku.base) return null;
    const variant = sku.base.find(item => item.propMap && item.propMap.includes(vid.toString()));
    return variant;
  };

  return (
    <div className="bg-white rounded-lg p-6 shadow-sm">
      <h3 className="font-semibold text-gray-900 mb-4">{colorProp.name || 'Variants'}</h3>
      
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
        {colorProp.values.map((value) => {
          const variantInfo = getVariantInfo(value.vid);
          const isSelected = selectedVariant === value.vid;
          const isAvailable = variantInfo && variantInfo.quantity > 0;
          
          return (
            <button
              key={value.vid}
              onClick={() => isAvailable && setSelectedVariant(value.vid)}
              disabled={!isAvailable}
              className={`relative border-2 rounded-lg p-3 transition-all ${
                isSelected
                  ? 'border-red-600 bg-red-50'
                  : isAvailable
                  ? 'border-gray-200 hover:border-gray-300'
                  : 'border-gray-200 opacity-50 cursor-not-allowed'
              }`}
            >
              {value.image && (
                <img
                  src={`https:${value.image}`}
                  alt={value.name}
                  className="w-full aspect-square object-cover rounded mb-2"
                  onError={(e) => {
                    e.target.style.display = 'none';
                  }}
                />
              )}
              
              <p className="text-sm font-medium text-gray-900 truncate">
                {value.name}
              </p>
              
              {variantInfo && (
                <div className="mt-1 text-xs">
                  <p className="text-red-600 font-semibold">
                    {formatPrice(variantInfo.promotionPrice || variantInfo.price)}
                  </p>
                  <p className="text-gray-500">
                    {variantInfo.quantity > 0 ? `${variantInfo.quantity} left` : 'Out of stock'}
                  </p>
                </div>
              )}
              
              {!isAvailable && (
                <div className="absolute inset-0 bg-gray-100 bg-opacity-75 rounded-lg flex items-center justify-center">
                  <span className="text-xs font-medium text-gray-600">Sold Out</span>
                </div>
              )}
            </button>
          );
        })}
      </div>
      
      {selectedVariant && (
        <div className="mt-4 p-4 bg-gray-50 rounded-lg">
          <p className="text-sm text-gray-600">Selected variant:</p>
          <p className="font-medium text-gray-900">
            {colorProp.values.find(v => v.vid === selectedVariant)?.name}
          </p>
        </div>
      )}
    </div>
  );
};

export default ProductSKU;