import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingBag } from 'lucide-react';
import { formatPrice } from '../../../shared/utils/formatters';

const CartSummary = ({ cart, selectedItems = new Set(), loading }) => {
  const navigate = useNavigate();

  // Filter only selected items
  const selectedCartItems = cart.items?.filter(item => selectedItems.has(item.id)) || [];

  // Calculate totals by currency for selected items only
  const totalsByCurrency = selectedCartItems.reduce((acc, item) => {
    const currency = item.currency || 'USD';
    // Ensure price is a number
    const price = typeof item.price === 'number' ? item.price : parseFloat(item.price) || 0;
    const itemTotal = price * item.quantity;

    if (!acc[currency]) {
      acc[currency] = 0;
    }
    acc[currency] += itemTotal;

    return acc;
  }, {});

  const totalItems = selectedCartItems.reduce((sum, item) => sum + item.quantity, 0);

  const handleCheckout = () => {
    navigate('/checkout');
  };

  // Get currency symbol
  const getCurrencySymbol = (currency) => {
    if (currency === 'CNY') return '¥';
    if (currency === 'USD') return '$';
    return currency;
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 sticky top-20">
      <h2 className="text-xl font-semibold mb-4">Tổng dự kiến</h2>

      <div className="space-y-3 mb-6">
        {/* Total Items */}
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">Sản phẩm đã chọn:</span>
          <span className="font-medium text-gray-900">{totalItems}</span>
        </div>

        {/* Subtotal by Currency */}
        <div className="pt-3 border-t border-gray-200">
          {Object.entries(totalsByCurrency).map(([currency, amount]) => (
            <div key={currency} className="mb-3">
              {/* Main Price */}
              <div className="flex justify-between items-baseline mb-1">
                <span className="text-sm text-gray-600">Tiền hàng:</span>
                <div className="text-right">
                  <div className="text-xl font-bold text-gray-900">
                    {formatPrice(amount, currency)}
                  </div>
                  <div className="text-xs text-gray-500">
                    {getCurrencySymbol(currency)}{(amount || 0).toFixed(2)}
                  </div>
                </div>
              </div>

              {/* Service Fee Placeholder */}
              <div className="flex justify-between items-baseline text-sm mb-1">
                <span className="text-gray-600">Phí dịch vụ:</span>
                <span className="text-gray-500">Cập nhật</span>
              </div>

              {/* Shipping Fee Placeholder */}
              <div className="flex justify-between items-baseline text-sm">
                <span className="text-gray-600">Phí vận chuyển:</span>
                <span className="text-gray-500">Cập nhật</span>
              </div>
            </div>
          ))}
        </div>

        {/* Note about selection */}
        {selectedItems.size === 0 && (
          <div className="text-xs text-orange-600 bg-orange-50 p-3 rounded">
            Vui lòng chọn sản phẩm để thanh toán
          </div>
        )}

        {/* Note about mixed currencies */}
        {Object.keys(totalsByCurrency).length > 1 && (
          <div className="text-xs text-gray-500 bg-yellow-50 p-3 rounded">
            Giỏ hàng có sản phẩm từ nhiều marketplace với đơn vị tiền tệ khác nhau.
          </div>
        )}
      </div>

      {/* Checkout Button */}
      <button
        onClick={handleCheckout}
        disabled={loading || selectedItems.size === 0}
        className="w-full py-3 bg-orange-500 text-white rounded-lg font-medium hover:bg-orange-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
      >
        <ShoppingBag className="w-5 h-5" />
        Thanh toán ({selectedItems.size})
      </button>

      {/* Continue Shopping Link */}
      <button
        onClick={() => navigate('/search')}
        className="w-full mt-3 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors"
      >
        Tiếp tục mua sắm
      </button>
    </div>
  );
};

export default CartSummary;
