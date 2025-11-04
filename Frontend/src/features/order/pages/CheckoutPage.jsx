import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCart } from '../../cart/services';
import { checkout } from '../services';
import { useCart } from '../../cart/context/CartContext';
import OrderItemsList from '../components/OrderItemsList';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import { formatPrice } from '../../../shared/utils/formatters';
import { normalizeMarketplace } from '../../../shared/utils/marketplace';
import { ShoppingBag } from 'lucide-react';

const CheckoutPage = () => {
  const navigate = useNavigate();
  const { refreshCart } = useCart();
  const [cart, setCart] = useState({ items: [] });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

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
        setCart(data);

        // Redirect if cart is empty
        if (!data.items || data.items.length === 0) {
          toast.info('Your cart is empty');
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
  }, [navigate]);

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
      const order = await checkout(formData);

      // Refresh cart count (should be 0 now)
      refreshCart();

      toast.success('Order placed successfully!');
      navigate(`/orders/success?orderNumber=${order.orderNumber || order.id}`);
    } catch (err) {
      console.error('Error placing order:', err);
      toast.error(err.response?.data?.message || 'Failed to place order');
    } finally {
      setSubmitting(false);
    }
  };

  // Calculate totals by currency
  const totalsByCurrency = cart.items?.reduce((acc, item) => {
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
              <h2 className="text-xl font-semibold mb-4">Shipping Information</h2>

              <div className="space-y-4">
                {/* Shipping Address */}
                <div>
                  <label htmlFor="shippingAddress" className="block text-sm font-medium text-gray-700 mb-2">
                    Shipping Address <span className="text-red-600">*</span>
                  </label>
                  <textarea
                    id="shippingAddress"
                    name="shippingAddress"
                    rows="3"
                    value={formData.shippingAddress}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent ${
                      formErrors.shippingAddress ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter your full shipping address"
                  />
                  {formErrors.shippingAddress && (
                    <p className="mt-1 text-sm text-red-600">{formErrors.shippingAddress}</p>
                  )}
                </div>

                {/* Phone */}
                <div>
                  <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-2">
                    Phone Number <span className="text-red-600">*</span>
                  </label>
                  <input
                    type="tel"
                    id="phone"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent ${
                      formErrors.phone ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="+1 234 567 8900"
                  />
                  {formErrors.phone && (
                    <p className="mt-1 text-sm text-red-600">{formErrors.phone}</p>
                  )}
                </div>

                {/* Note */}
                <div>
                  <label htmlFor="note" className="block text-sm font-medium text-gray-700 mb-2">
                    Order Note (Optional)
                  </label>
                  <textarea
                    id="note"
                    name="note"
                    rows="2"
                    value={formData.note}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                    placeholder="Any special instructions for your order"
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
                <span>Total Items:</span>
                <span className="font-medium text-gray-900">{totalItems}</span>
              </div>

              <div className="pt-3 border-t border-gray-200">
                {Object.entries(totalsByCurrency).map(([currency, amount]) => (
                  <div key={currency} className="flex justify-between items-baseline mb-2">
                    <span className="text-gray-600">Total ({currency}):</span>
                    <span className="text-xl font-bold text-gray-900">
                      {formatPrice(amount, currency)}
                    </span>
                  </div>
                ))}
              </div>

              {Object.keys(totalsByCurrency).length > 1 && (
                <div className="text-xs text-gray-500 bg-yellow-50 p-2 rounded">
                  Note: Multiple currencies detected
                </div>
              )}
            </div>

            {/* Order Items */}
            <div className="border-t border-gray-200 pt-4">
              <h3 className="text-sm font-medium text-gray-900 mb-3">Items in your order</h3>
              <OrderItemsList items={cart.items} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;
