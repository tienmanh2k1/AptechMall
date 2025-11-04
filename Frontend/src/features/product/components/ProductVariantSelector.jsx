import React, { useState, useEffect, useMemo } from 'react';
import { Check } from 'lucide-react';

const ProductVariantSelector = ({ attributes, configuredItems, onVariantChange }) => {
  if (!attributes || attributes.length === 0) {
    return null;
  }

  // Memoize configurator attributes
  const configuratorAttrs = useMemo(() => {
    return attributes.filter(attr => attr.IsConfigurator);
  }, [attributes]);

  // Memoize property groups to prevent recalculation on every render
  const propertyGroups = useMemo(() => {
    const groups = {};
    configuratorAttrs.forEach(attr => {
      if (!groups[attr.PropertyName]) {
        groups[attr.PropertyName] = {
          propertyId: attr.Pid,
          propertyName: attr.PropertyName,
          options: []
        };
      }

      // Add unique options only
      const existingOption = groups[attr.PropertyName].options.find(
        opt => opt.valueId === attr.Vid
      );

      if (!existingOption) {
        groups[attr.PropertyName].options.push({
          valueId: attr.Vid,
          value: attr.Value,
          valueAlias: attr.ValueAlias || attr.Value,
          imageUrl: attr.ImageUrl,
          miniImageUrl: attr.MiniImageUrl
        });
      }
    });
    return groups;
  }, [configuratorAttrs]);

  // State to track selected options for each property
  const [selectedOptions, setSelectedOptions] = useState({});
  const [isInitialized, setIsInitialized] = useState(false);

  // Initialize with first option of each property (only once)
  useEffect(() => {
    // Skip if already initialized
    if (isInitialized) return;
    if (Object.keys(propertyGroups).length === 0) return;

    const initialSelections = {};
    Object.entries(propertyGroups).forEach(([propertyName, group]) => {
      if (group.options.length > 0) {
        initialSelections[group.propertyId] = group.options[0].valueId;
      }
    });

    setSelectedOptions(initialSelections);
    setIsInitialized(true);
  }, [propertyGroups, isInitialized]);

  // When selections change, find matching configured item
  useEffect(() => {
    if (Object.keys(selectedOptions).length === 0) return;
    if (!configuredItems) return;
    if (!onVariantChange) return;

    // Find configured item that matches selected options
    const matchingItem = configuredItems.find(item => {
      const configurators = item.Configurators || [];

      // Check if all selected options match this item's configurators
      return Object.entries(selectedOptions).every(([pid, vid]) => {
        return configurators.some(conf =>
          conf.Pid === pid && conf.Vid === vid
        );
      });
    });

    if (matchingItem) {
      // Find the attribute with image for the selected color/variant
      const selectedImageAttr = configuratorAttrs.find(attr =>
        attr.ImageUrl && selectedOptions[attr.Pid] === attr.Vid
      );

      onVariantChange({
        configuredItemId: matchingItem.Id,
        price: matchingItem.Price?.ConvertedPriceWithoutSign || matchingItem.Price?.OriginalPrice,
        quantity: matchingItem.Quantity,
        selectedOptions,
        variantImage: selectedImageAttr?.ImageUrl
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedOptions]); // Only re-run when selectedOptions changes

  const handleOptionSelect = (propertyId, valueId) => {
    setSelectedOptions(prev => ({
      ...prev,
      [propertyId]: valueId
    }));
  };

  return (
    <div className="space-y-6">
      {Object.entries(propertyGroups).map(([propertyName, group]) => (
        <div key={propertyName} className="border-b border-gray-200 pb-6 last:border-b-0">
          <h4 className="text-sm font-semibold text-gray-900 mb-3">
            {propertyName}
          </h4>

          <div className="flex flex-wrap gap-2">
            {group.options.map((option) => {
              const isSelected = selectedOptions[group.propertyId] === option.valueId;

              return (
                <button
                  key={option.valueId}
                  onClick={() => handleOptionSelect(group.propertyId, option.valueId)}
                  className={`
                    relative px-4 py-2 rounded-lg border-2 transition-all
                    ${isSelected
                      ? 'border-red-600 bg-red-50 text-red-900'
                      : 'border-gray-200 bg-white text-gray-700 hover:border-gray-300'
                    }
                  `}
                >
                  {/* Image option */}
                  {option.imageUrl && (
                    <div className="flex items-center gap-2">
                      <img
                        src={option.imageUrl?.startsWith('http') ? option.imageUrl : `https:${option.imageUrl}`}
                        alt={option.valueAlias}
                        className="w-12 h-12 rounded object-cover"
                        onError={(e) => {
                          e.target.style.display = 'none';
                        }}
                      />
                      <span className="text-sm font-medium">{option.valueAlias}</span>
                    </div>
                  )}

                  {/* Text option */}
                  {!option.imageUrl && (
                    <span className="text-sm font-medium">{option.valueAlias}</span>
                  )}

                  {/* Check icon for selected */}
                  {isSelected && (
                    <div className="absolute -top-1 -right-1 bg-red-600 rounded-full p-0.5">
                      <Check className="w-3 h-3 text-white" />
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
};

export default ProductVariantSelector;
