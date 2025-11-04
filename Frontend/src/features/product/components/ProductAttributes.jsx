import React from 'react';
import { Package, Tag } from 'lucide-react';

const ProductAttributes = ({ attributes }) => {
  if (!attributes || attributes.length === 0) {
    return null;
  }

  // Separate configurator attributes (variants) from regular attributes (specs)
  const configuratorAttrs = attributes.filter(attr => attr.IsConfigurator);
  const specAttrs = attributes.filter(attr => !attr.IsConfigurator);

  return (
    <div className="bg-white rounded-lg p-6 shadow-sm">
      <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <Package className="w-5 h-5 text-red-600" />
        Product Attributes
      </h3>

      {/* Configurator Attributes (Variants like Color, Size, etc.) */}
      {configuratorAttrs.length > 0 && (
        <div className="mb-6">
          <h4 className="text-sm font-medium text-gray-700 mb-3 flex items-center gap-2">
            <Tag className="w-4 h-4" />
            Available Options
          </h4>
          <div className="space-y-4">
            {configuratorAttrs.map((attr, index) => (
              <div key={index} className="border-l-2 border-red-200 pl-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900">
                      {attr.PropertyName}
                    </p>
                    <p className="text-sm text-gray-600 mt-1">
                      {attr.ValueAlias || attr.Value}
                    </p>
                  </div>
                  {attr.ImageUrl && (
                    <img
                      src={attr.ImageUrl?.startsWith('http') ? attr.ImageUrl : `https:${attr.ImageUrl}`}
                      alt={attr.Value}
                      className="w-16 h-16 rounded-lg object-cover border border-gray-200"
                      onError={(e) => {
                        e.target.style.display = 'none';
                      }}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Regular Specification Attributes */}
      {specAttrs.length > 0 && (
        <div>
          <h4 className="text-sm font-medium text-gray-700 mb-3">
            Specifications
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {specAttrs.map((attr, index) => (
              <div
                key={index}
                className="flex justify-between items-start py-2 px-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <span className="text-sm text-gray-600 font-medium">
                  {attr.PropertyName}:
                </span>
                <span className="text-sm text-gray-900 ml-2 text-right">
                  {attr.ValueAlias || attr.Value}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* If no configurators and no specs, show all attributes */}
      {configuratorAttrs.length === 0 && specAttrs.length === 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {attributes.map((attr, index) => (
            <div
              key={index}
              className="flex justify-between items-start py-2 px-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <span className="text-sm text-gray-600 font-medium">
                {attr.PropertyName}:
              </span>
              <span className="text-sm text-gray-900 ml-2 text-right">
                {attr.ValueAlias || attr.Value}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ProductAttributes;
