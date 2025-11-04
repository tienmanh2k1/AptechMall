import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import OrderStatusBadge from './OrderStatusBadge';
import { formatPrice } from '../../../shared/utils/formatters';
import { normalizeMarketplace } from '../../../shared/utils/marketplace';
import PriceDisplay from '../../currency/components/PriceDisplay';

const OrderItem = ({ order }) => {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/orders/${order.id}`);
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateString;
    }
  };

  // Calculate total by currency
  const totalsByCurrency = order.items?.reduce((acc, item) => {
    // Infer currency from marketplace if not provided by backend
    const platform = normalizeMarketplace(item.platform || item.marketplace);
    const currency = item.currency || (platform === 'aliexpress' ? 'USD' : 'CNY');
    const itemTotal = item.price * item.quantity;

    if (!acc[currency]) {
      acc[currency] = 0;
    }
    acc[currency] += itemTotal;

    return acc;
  }, {}) || {};

  const totalItems = order.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;

  return (
    <div
      onClick={handleClick}
      className="bg-white rounded-lg border border-gray-200 p-6 hover:shadow-md transition-shadow cursor-pointer"
    >
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <div className="flex items-center gap-3 mb-2">
            <h3 className="text-lg font-semibold text-gray-900">
              Order #{order.orderNumber || order.id}
            </h3>
            <OrderStatusBadge status={order.status} />
          </div>
          <p className="text-sm text-gray-600">
            Placed on {formatDate(order.createdAt)}
          </p>
        </div>
        <ChevronRight className="w-5 h-5 text-gray-400" />
      </div>

      <div className="border-t border-gray-200 pt-4">
        <div className="flex justify-between items-center mb-2">
          <span className="text-sm text-gray-600">Total Items:</span>
          <span className="text-sm font-medium text-gray-900">{totalItems}</span>
        </div>

        {Object.entries(totalsByCurrency).map(([currency, amount]) => (
          <div key={currency} className="flex justify-between items-center">
            <span className="text-sm text-gray-600">Total ({currency}):</span>
            <PriceDisplay
              price={amount}
              currency={currency}
              showBoth={true}
              size="text-lg"
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default OrderItem;
