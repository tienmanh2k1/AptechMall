import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingBag } from 'lucide-react';
import { useCurrency } from '../../currency/context/CurrencyContext';
import { formatCurrency } from '../../currency/services/currencyApi';

const CartSummary = ({ cart, selectedItems = new Set(), loading }) => {
  const navigate = useNavigate();
  const { toVND, exchangeRates } = useCurrency();

  // Filter only selected items
  const selectedCartItems = cart.items?.filter(item => selectedItems.has(item.id)) || [];

  // Calculate total in VND for selected items
  let totalVND = 0;
  for (const item of selectedCartItems) {
    const currency = item.currency || 'USD';
    const price = typeof item.price === 'number' ? item.price : parseFloat(item.price) || 0;
    const itemTotal = price * item.quantity;

    const vndAmount = toVND(itemTotal, currency);
    if (vndAmount !== null) {
      totalVND += vndAmount;
    }
  }

  const totalItems = selectedCartItems.reduce((sum, item) => sum + item.quantity, 0);

  // Calculate service fee (1.5% of product cost)
  const serviceFeeVND = totalVND * 0.015;

  // Calculate total with service fee
  const totalWithServiceFee = totalVND + serviceFeeVND;

  // Calculate 70% deposit on (product + service fee)
  const depositVND = totalWithServiceFee * 0.70;

  const handleCheckout = () => {
    // Pass selected item IDs to checkout page via navigation state
    const selectedItemIds = Array.from(selectedItems);
    navigate('/checkout', {
      state: { selectedItemIds }
    });
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 sticky top-20">
      <h2 className="text-xl font-semibold mb-4">Tổng dự kiến</h2>

      <div className="space-y-3 mb-6">
        {/* Total Items */}
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">Sản phẩm đã chọn:</span>
          <span className="font-medium text-gray-900">{totalItems} mặt hàng</span>
        </div>

        {/* Fee Breakdown */}
        <div className="pt-3 border-t border-gray-200">
          <p className="text-sm font-medium text-gray-700 mb-3">Tổng dự kiến:</p>
          <div className="space-y-2 text-sm">
            {/* 1. Tiền hàng */}
            <div className="flex justify-between text-gray-600">
              <span>1. Tiền hàng:</span>
              <span className="font-medium">
                {exchangeRates ? formatCurrency(totalVND, 'VND') : 'Đang tải...'}
              </span>
            </div>

            {/* 2. Phí mua hàng */}
            <div className="flex justify-between text-gray-600">
              <span>2. Phí mua hàng (1.5%):</span>
              <span className="font-medium">
                {exchangeRates ? formatCurrency(serviceFeeVND, 'VND') : 'Đang tải...'}
              </span>
            </div>

            {/* 3. Phí vận chuyển nội địa TQ */}
            <div className="flex justify-between text-gray-600">
              <span>3. Phí vận chuyển nội địa TQ:</span>
              <span className="text-gray-400 italic text-xs">cập nhật</span>
            </div>

            {/* 4. Phí vận chuyển quốc tế */}
            <div className="flex justify-between text-gray-600">
              <span>4. Phí vận chuyển quốc tế:</span>
              <span className="text-gray-400 italic text-xs">cập nhật</span>
            </div>

            {/* 5. Phí dịch vụ bổ sung */}
            <div className="flex justify-between text-gray-600">
              <span>5. Phí dịch vụ:</span>
              <span className="text-gray-400 italic text-xs">cập nhật</span>
            </div>

            {/* Deposit */}
            <div className="flex justify-between items-baseline pt-2 border-t border-gray-200">
              <div>
                <div className="text-sm text-gray-600">Tiền cọc (70%)</div>
                <div className="text-xs text-gray-500">Thanh toán khi đặt hàng</div>
              </div>
              <span className="text-lg font-bold text-blue-600">
                {exchangeRates ? formatCurrency(depositVND, 'VND') : 'Đang tải...'}
              </span>
            </div>
          </div>
        </div>

        {/* Note about selection */}
        {selectedItems.size === 0 && (
          <div className="text-xs text-orange-600 bg-orange-50 p-3 rounded">
            Vui lòng chọn sản phẩm để thanh toán
          </div>
        )}

        {/* Loading exchange rates */}
        {!exchangeRates && selectedItems.size > 0 && (
          <div className="text-xs text-blue-600 bg-blue-50 p-3 rounded">
            Đang tải tỷ giá...
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
