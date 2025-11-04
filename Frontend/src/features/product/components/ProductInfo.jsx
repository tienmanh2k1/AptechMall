import React from 'react';
import { Star } from 'lucide-react';
import PriceDisplay from '../../currency/components/PriceDisplay';

const ProductInfo = ({ product, seller, platform, productId }) => {
  // Handle both normalized format (product.result.item) and direct item prop
  const item = product?.result?.item || product?.Result?.Item || product;

  // Get currency from product (default to USD for AliExpress, CNY for 1688)
  const currency = product?.Result?.Item?.Price?.Currency ||
                   product?.currency ||
                   (product?.platform === '1688' ? 'CNY' : 'USD');

  if (!item || !item.title) {
    return <div>Product information not available</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{item.title}</h1>

        {/* View on Marketplace Links - Same Row */}
        <div className="flex items-center gap-3 mb-4">
          {/* Link to Seller's Shop */}
          {seller?.storeUrl && seller?.storeTitle && (
            <a
              href={`https:${seller.storeUrl}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center px-3 py-1.5 bg-gradient-to-r from-orange-400 to-orange-500 text-white text-xs font-medium rounded hover:from-orange-500 hover:to-orange-600 transition-all"
            >
              <span>Xem sản phẩm nhà bán: {seller.storeTitle}</span>
              <svg className="w-3 h-3 ml-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}

          {/* Link to Product on Original Marketplace */}
          {productId && platform && (
            <a
              href={
                platform === '1688'
                  ? `https://detail.1688.com/offer/${productId}.html`
                  : `https://www.aliexpress.com/item/${productId}.html`
              }
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center px-3 py-1.5 bg-gradient-to-r from-orange-400 to-orange-500 text-white text-xs font-medium rounded hover:from-orange-500 hover:to-orange-600 transition-all"
            >
              <span>Xem sản phẩm trên {platform === '1688' ? '1688.com' : 'AliExpress.com'}</span>
              <svg className="w-3 h-3 ml-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}
        </div>

        <div className="flex items-center space-x-4 text-sm text-gray-600">
          <div className="flex items-center">
            <Star className="w-4 h-4 text-yellow-400 fill-current mr-1" />
            <span>4.8</span>
            <span className="ml-1">(2.5k reviews)</span>
          </div>
          <div>
            <span className="font-medium">{item.sku?.def?.quantity || 0}</span> pieces available
          </div>
        </div>
      </div>
      
      <div className="border-t border-b border-gray-200 py-6">
        <div className="space-y-4">

          {item.sku?.def?.promotionPrice && item.sku.def.promotionPrice !== item.sku.def.price && (
            <div className="pt-4 border-t border-gray-100">
              <PriceDisplay
                price={parseFloat(item.sku.def.promotionPrice)}
                currency={currency}
                showBoth={true}
                size="text-3xl"
                className="mb-2"
              />
            </div>
          )}
        </div>
      </div>
      
      <div className="space-y-4">
        <h3 className="font-semibold text-gray-900">Product Details</h3>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-600">Item ID:</span>
            <span className="ml-2 font-medium">{item.itemId}</span>
          </div>
          <div>
            <span className="text-gray-600">Category:</span>
            <span className="ml-2 font-medium">
              {item.breadcrumbs && item.breadcrumbs[1]?.title ? item.breadcrumbs[1].title : 'N/A'}
            </span>
          </div>
          <div>
            <span className="text-gray-600">Unit:</span>
            <span className="ml-2 font-medium">{item.sku?.def?.unit || 'piece'}</span>
          </div>
          <div>
            <span className="text-gray-600">Bulk Order:</span>
            <span className="ml-2 font-medium">{item.sku?.def?.isBulk ? 'Yes' : 'No'}</span>
          </div>
        </div>
      </div>
      
    </div>
  );
};

export default ProductInfo;