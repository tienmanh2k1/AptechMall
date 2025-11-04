import React from 'react';
import { Truck, Clock } from 'lucide-react';

const ProductShipping = ({ delivery }) => {
  if (!delivery || !delivery.shippingList || delivery.shippingList.length === 0) {
    return null;
  }

  const shipping = delivery.shippingList[0];

  return (
    <div className="bg-white rounded-lg p-6 shadow-sm">
      <h3 className="font-semibold text-gray-900 mb-4">Shipping Information</h3>
      
      <div className="space-y-4">
        <div className="flex items-start space-x-3">
          <Truck className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
          <div className="flex-1">
            <p className="font-medium text-gray-900">Shipping Method</p>
            <p className="text-sm text-gray-600">{shipping.shippingCompany}</p>
            <p className="text-sm text-red-600 font-medium mt-1">
              ${shipping.shippingFee}
            </p>
          </div>
        </div>
        
        <div className="flex items-start space-x-3">
          <Clock className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
          <div className="flex-1">
            <p className="font-medium text-gray-900">Estimated Delivery</p>
            <p className="text-sm text-gray-600">
              {shipping.shippingTime} days ({shipping.estimateDelivery})
            </p>
            {shipping.trackingAvailable && (
              <p className="text-sm text-green-600 mt-1">✓ Tracking available</p>
            )}
          </div>
        </div>
        
        <div className="border-t pt-4">
          <p className="text-sm text-gray-600">
            <span className="font-medium">From:</span> {delivery.shippingFrom}
          </p>
          <p className="text-sm text-gray-600">
            <span className="font-medium">To:</span> {delivery.shippingTo}
          </p>
        </div>
      </div>
    </div>
  );
};

export default ProductShipping;