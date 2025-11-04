import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getOrderById, cancelOrder } from '../services';
import OrderStatusBadge from '../components/OrderStatusBadge';
import OrderItemsList from '../components/OrderItemsList';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import { formatPrice } from '../../../shared/utils/formatters';
import { normalizeMarketplace } from '../../../shared/utils/marketplace';
import { ArrowLeft, MapPin, Phone, FileText } from 'lucide-react';

const OrderDetailPage = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const fetchOrder = async () => {
    try {
      setLoading(true);
      setError(null);
      // User ID is automatically extracted from JWT token by backend
      const data = await getOrderById(orderId);
      setOrder(data);
    } catch (err) {
      console.error('Error fetching order:', err);
      setError(err.response?.data?.message || 'Failed to load order');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrder();
  }, [orderId]);

  const handleCancelOrder = async () => {
    if (!window.confirm('Are you sure you want to cancel this order?')) {
      return;
    }

    try {
      setCancelling(true);
      // User ID is automatically extracted from JWT token by backend
      await cancelOrder(orderId);
      toast.success('Order cancelled successfully');
      fetchOrder(); // Refresh order data
    } catch (err) {
      console.error('Error cancelling order:', err);
      toast.error(err.response?.data?.message || 'Failed to cancel order');
    } finally {
      setCancelling(false);
    }
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateString;
    }
  };

  // Loading state
  if (loading) {
    return <Loading message="Loading order details..." />;
  }

  // Error state
  if (error) {
    return (
      <div className="container mx-auto px-4 py-16">
        <ErrorMessage
          message={error}
          onRetry={fetchOrder}
        />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <p className="text-gray-600">Order not found</p>
      </div>
    );
  }

  // Calculate totals by currency
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
  const canCancel = order.status === 'PENDING';

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Back Button */}
      <button
        onClick={() => navigate('/orders')}
        className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6 transition-colors"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to Orders
      </button>

      {/* Order Header */}
      <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 mb-2">
              Order #{order.orderNumber || order.id}
            </h1>
            <p className="text-gray-600">
              Placed on {formatDate(order.createdAt)}
            </p>
          </div>
          <OrderStatusBadge status={order.status} />
        </div>

        {/* Cancel Button */}
        {canCancel && (
          <button
            onClick={handleCancelOrder}
            disabled={cancelling}
            className="w-full sm:w-auto px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {cancelling ? 'Cancelling...' : 'Cancel Order'}
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Order Items */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-xl font-semibold mb-4">Order Items</h2>
            <OrderItemsList items={order.items} />
          </div>

          {/* Shipping Information */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-xl font-semibold mb-4">Shipping Information</h2>
            <div className="space-y-3">
              <div className="flex items-start gap-3">
                <MapPin className="w-5 h-5 text-gray-400 mt-1" />
                <div>
                  <p className="text-sm text-gray-600 mb-1">Address</p>
                  <p className="text-gray-900">{order.shippingAddress || 'N/A'}</p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Phone className="w-5 h-5 text-gray-400 mt-1" />
                <div>
                  <p className="text-sm text-gray-600 mb-1">Phone</p>
                  <p className="text-gray-900">{order.phone || 'N/A'}</p>
                </div>
              </div>

              {order.note && (
                <div className="flex items-start gap-3">
                  <FileText className="w-5 h-5 text-gray-400 mt-1" />
                  <div>
                    <p className="text-sm text-gray-600 mb-1">Order Note</p>
                    <p className="text-gray-900">{order.note}</p>
                  </div>
                </div>
              )}
            </div>
          </div>
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
                    <span className="text-xl font-bold text-red-600">
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

            <div className="text-sm text-gray-500 border-t border-gray-200 pt-4">
              <p>Need help with your order?</p>
              <p className="mt-1">Contact our customer support</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderDetailPage;
