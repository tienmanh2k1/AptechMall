import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCart } from '../../cart/services';
import { checkout } from '../services';
import { useCart } from '../../cart/context/CartContext';
import { useCurrency } from '../../currency/context/CurrencyContext';
import { formatCurrency } from '../../currency/services/currencyApi';
import { getDefaultAddress } from '../../user/services/addressApi';
import OrderItemsList from '../components/OrderItemsList';
import AddressSelectorModal from '../components/AddressSelectorModal';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import { ShoppingBag, Wallet, MapPin, User, Phone, Edit2 } from 'lucide-react';

const CheckoutPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { refreshCart } = useCart();
  const { toVND, exchangeRates } = useCurrency();

  // Get selected item IDs from navigation state (passed from CartSummary)
  const selectedItemIds = location.state?.selectedItemIds || [];

  const [cart, setCart] = useState({ items: [] });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Address state
  const [selectedAddress, setSelectedAddress] = useState(null);
  const [showAddressModal, setShowAddressModal] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    shippingAddress: '',
    phone: '',
    note: ''
  });

  const [formErrors, setFormErrors] = useState({});

  // Fetch cart data
  useEffect(() => {
    const fetchCart = async () => {
      try {
        setLoading(true);
        setError(null);
        // User ID is automatically extracted from JWT token by backend
        const data = await getCart();

        // Filter cart to only include selected items
        const filteredCart = {
          ...data,
          items: selectedItemIds.length > 0
            ? data.items?.filter(item => selectedItemIds.includes(item.id)) || []
            : data.items || []
        };

        setCart(filteredCart);

        // Redirect if no items selected or cart is empty
        if (!filteredCart.items || filteredCart.items.length === 0) {
          toast.info(selectedItemIds.length > 0 ? 'Selected items not found in cart' : 'Your cart is empty');
          navigate('/cart');
        }
      } catch (err) {
        console.error('Error fetching cart:', err);
        setError(err.response?.data?.message || 'Failed to load cart');
      } finally {
        setLoading(false);
      }
    };

    fetchCart();
  }, [navigate, selectedItemIds]);

  // Fetch default address
  useEffect(() => {
    const fetchDefaultAddr = async () => {
      try {
        const defaultAddr = await getDefaultAddress();
        if (defaultAddr) {
          setSelectedAddress(defaultAddr);
          setFormData(prev => ({
            ...prev,
            shippingAddress: defaultAddr.fullAddress,
            phone: defaultAddr.phone
          }));
        }
      } catch (error) {
        console.error('Error fetching default address:', error);
        // Not critical, just log the error
      }
    };

    fetchDefaultAddr();
  }, []);

  // Handle address selection
  const handleSelectAddress = (address) => {
    setSelectedAddress(address);
    setFormData(prev => ({
      ...prev,
      shippingAddress: address.fullAddress,
      phone: address.phone
    }));
    setFormErrors(prev => ({
      ...prev,
      shippingAddress: '',
      phone: ''
    }));
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // Clear error for this field
    if (formErrors[name]) {
      setFormErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  // Validate form
  const validateForm = () => {
    const errors = {};

    if (!formData.shippingAddress.trim()) {
      errors.shippingAddress = 'Shipping address is required';
    }

    if (!formData.phone.trim()) {
      errors.phone = 'Phone number is required';
    } else if (!/^[\d\s\-+()]{8,}$/.test(formData.phone)) {
      errors.phone = 'Please enter a valid phone number';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle form submit
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error('Please fix the form errors');
      return;
    }

    try {
      setSubmitting(true);
      // User ID is automatically extracted from JWT token by backend
      // Include selected item IDs in checkout request
      const checkoutData = {
        ...formData,
        itemIds: selectedItemIds.length > 0 ? selectedItemIds : undefined
      };
      const order = await checkout(checkoutData);

      // Refresh cart count (items will be removed from cart)
      refreshCart();

      toast.success('Order placed successfully!');
      navigate(`/orders/success?orderNumber=${order.orderNumber || order.id}`);
    } catch (err) {
      console.error('Error placing order:', err);
      const errorMessage = err.response?.data?.message || 'Failed to place order';

      // Check if error is insufficient funds
      if (errorMessage.toLowerCase().includes('insufficient')) {
        // Show error with deposit link
        toast.error(
          <div>
            <div className="font-medium mb-1">Insufficient Wallet Balance</div>
            <div className="text-sm">{errorMessage}</div>
            <button
              onClick={() => navigate('/wallet')}
              className="mt-2 text-xs underline text-white hover:text-blue-200"
            >
              Go to Wallet to Deposit
            </button>
          </div>,
          { autoClose: false }
        );
      } else {
        toast.error(errorMessage);
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Calculate total in VND
  let totalVND = 0;
  if (cart.items && exchangeRates) {
    for (const item of cart.items) {
      const currency = item.currency || 'USD';
      const itemTotal = item.price * item.quantity;
      const vndAmount = toVND(itemTotal, currency);
      if (vndAmount !== null) {
        totalVND += vndAmount;
      }
    }
  }

  // Calculate service fee (1.5% of product cost)
  const serviceFeeVND = totalVND * 0.015;

  // Calculate total with service fee
  const totalWithServiceFee = totalVND + serviceFeeVND;

  // Calculate 70% deposit on (product + service fee)
  const depositVND = totalWithServiceFee * 0.70;
  const remainingVND = totalWithServiceFee * 0.30;

  const totalItems = cart.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;

  // Loading state
  if (loading) {
    return <Loading message="Loading checkout..." />;
  }

  // Error state
  if (error) {
    return (
      <div className="container mx-auto px-4 py-16">
        <ErrorMessage
          message={error}
          onRetry={() => window.location.reload()}
        />
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">Checkout</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Checkout Form */}
        <div className="lg:col-span-2">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Shipping Information */}
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold">Thông tin giao hàng</h2>
                <button
                  type="button"
                  onClick={() => setShowAddressModal(true)}
                  className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-primary-600 hover:text-primary-700 border border-primary-300 rounded-lg hover:bg-primary-50 transition-colors"
                >
                  <Edit2 className="w-4 h-4 mr-1" />
                  {selectedAddress ? 'Thay đổi' : 'Chọn địa chỉ'}
                </button>
              </div>

              {selectedAddress ? (
                <div className="mb-4 p-4 border-2 border-primary-500 bg-primary-50 rounded-lg">
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <User className="w-4 h-4 text-gray-600" />
                      <span className="font-medium text-gray-900">
                        {selectedAddress.receiverName}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Phone className="w-4 h-4 text-gray-600" />
                      <span className="text-gray-700">{selectedAddress.phone}</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <MapPin className="w-4 h-4 text-gray-600 mt-1 flex-shrink-0" />
                      <p className="text-gray-700 text-sm">
                        {selectedAddress.fullAddress}
                      </p>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="mb-4 p-4 border-2 border-dashed border-gray-300 rounded-lg text-center">
                  <MapPin className="w-8 h-8 mx-auto text-gray-400 mb-2" />
                  <p className="text-sm text-gray-600">
                    Chưa chọn địa chỉ giao hàng
                  </p>
                  <button
                    type="button"
                    onClick={() => setShowAddressModal(true)}
                    className="mt-2 text-sm text-primary-600 hover:text-primary-700 font-medium"
                  >
                    Click để chọn địa chỉ
                  </button>
                </div>
              )}

              <div className="space-y-4">
                {/* Hidden fields for form validation */}
                <input
                  type="hidden"
                  name="shippingAddress"
                  value={formData.shippingAddress}
                />
                <input type="hidden" name="phone" value={formData.phone} />

                {formErrors.shippingAddress && (
                  <p className="text-sm text-red-600">{formErrors.shippingAddress}</p>
                )}
                {formErrors.phone && (
                  <p className="text-sm text-red-600">{formErrors.phone}</p>
                )}

                {/* Note */}
                <div>
                  <label htmlFor="note" className="block text-sm font-medium text-gray-700 mb-2">
                    Ghi chú đơn hàng (Không bắt buộc)
                  </label>
                  <textarea
                    id="note"
                    name="note"
                    rows="2"
                    value={formData.note}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                    placeholder="Ghi chú đặc biệt cho đơn hàng của bạn"
                  />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {submitting ? (
                <>Processing...</>
              ) : (
                <>
                  <ShoppingBag className="w-5 h-5" />
                  Place Order
                </>
              )}
            </button>
          </form>
        </div>

        {/* Order Summary */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg border border-gray-200 p-6 sticky top-20">
            <h2 className="text-xl font-semibold mb-4">Order Summary</h2>

            <div className="space-y-3 mb-6">
              <div className="flex justify-between text-gray-600">
                <span>Sản phẩm:</span>
                <span className="font-medium text-gray-900">{totalItems} mặt hàng</span>
              </div>

              <div className="pt-3 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-700 mb-3">Tổng dự kiến:</p>
                <div className="space-y-2 text-sm">
                  {/* 1. Tiền hàng */}
                  <div className="flex justify-between text-gray-600">
                    <span>1. Tiền hàng:</span>
                    <span className="font-medium">
                      {exchangeRates ? formatCurrency(totalVND, 'VND') : 'Loading...'}
                    </span>
                  </div>

                  {/* 2. Phí mua hàng */}
                  <div className="flex justify-between text-gray-600">
                    <span>2. Phí mua hàng (1.5%):</span>
                    <span className="font-medium">
                      {exchangeRates ? formatCurrency(serviceFeeVND, 'VND') : 'Loading...'}
                    </span>
                  </div>

                  {/* 3. Phí vận chuyển nội địa TQ */}
                  <div className="flex justify-between text-gray-600">
                    <span>3. Phí vận chuyển nội địa TQ:</span>
                    <span className="text-gray-400 italic text-xs">cập nhật</span>
                  </div>

                  {/* 4. Phí vận chuyển quốc tế */}
                  <div className="flex justify-between text-gray-600">
                    <span>4. Phí vận chuyển quốc tế TQ - VN:</span>
                    <span className="text-gray-400 italic text-xs">cập nhật</span>
                  </div>

                  {/* 5. Phí dịch vụ bổ sung */}
                  <div className="flex justify-between text-gray-600">
                    <span>5. Phí dịch vụ:</span>
                    <span className="text-gray-400 italic text-xs">cập nhật</span>
                  </div>

                  {/* Total line */}
                  <div className="flex justify-between text-gray-900 font-semibold pt-2 border-t border-gray-200">
                    <span>Tiền hàng + phí mua hàng:</span>
                    <span className="text-base">
                      {exchangeRates ? formatCurrency(totalWithServiceFee, 'VND') : 'Loading...'}
                    </span>
                  </div>

                  {/* Deposit */}
                  <div className="flex justify-between items-baseline pt-2 border-t border-gray-100">
                    <div>
                      <div className="text-sm font-medium text-blue-600">Tiền cọc (70%)</div>
                      <div className="text-xs text-gray-500">Thanh toán ngay từ ví</div>
                    </div>
                    <span className="text-xl font-bold text-blue-600">
                      {exchangeRates ? formatCurrency(depositVND, 'VND') : 'Loading...'}
                    </span>
                  </div>

                  {/* Remaining */}
                  <div className="flex justify-between items-baseline text-sm">
                    <span className="text-gray-500">Còn lại (30% + phí):</span>
                    <span className="text-gray-600 font-medium">
                      {exchangeRates ? formatCurrency(remainingVND, 'VND') : 'Loading...'}
                    </span>
                  </div>
                </div>
              </div>

              {!exchangeRates && (
                <div className="text-xs text-blue-600 bg-blue-50 p-2 rounded">
                  Loading exchange rates...
                </div>
              )}

              <div className="text-xs text-gray-500 bg-gray-50 p-3 rounded">
                <Wallet className="w-4 h-4 inline mr-1" />
                Payment will be deducted from your wallet. Remaining 30% + fees will be charged later.
              </div>
            </div>

            {/* Order Items */}
            <div className="border-t border-gray-200 pt-4">
              <h3 className="text-sm font-medium text-gray-900 mb-3">Items in your order</h3>
              <OrderItemsList items={cart.items} />
            </div>
          </div>
        </div>
      </div>

      {/* Address Selector Modal */}
      <AddressSelectorModal
        isOpen={showAddressModal}
        onClose={() => setShowAddressModal(false)}
        onSelectAddress={handleSelectAddress}
        selectedAddressId={selectedAddress?.id}
      />
    </div>
  );
};

export default CheckoutPage;
