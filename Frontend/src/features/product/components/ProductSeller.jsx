import React from 'react';
import { Store, ExternalLink } from 'lucide-react';

const ProductSeller = ({ seller }) => {
  if (!seller) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg p-6 shadow-sm">
      <h3 className="font-semibold text-gray-900 mb-4">Seller Information</h3>
      
      <div className="flex items-start space-x-4">
        {seller.storeImage && (
          <img
            src={`https:${seller.storeImage}`}
            alt={seller.storeTitle}
            className="w-16 h-16 rounded-lg object-cover"
            onError={(e) => {
              e.target.style.display = 'none';
            }}
          />
        )}
        
        <div className="flex-1">
          <h4 className="font-medium text-gray-900">{seller.storeTitle}</h4>
          <p className="text-sm text-gray-600">Store ID: {seller.storeId}</p>
          
          <a
            href={`https:${seller.storeUrl}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center space-x-1 text-sm text-red-600 hover:text-red-700 mt-2"
          >
            <Store className="w-4 h-4" />
            <span>Visit Store</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </div>
    </div>
  );
};

export default ProductSeller;