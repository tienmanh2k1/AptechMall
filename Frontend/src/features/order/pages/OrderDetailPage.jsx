import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getOrderById, cancelOrder, payRemainingAmount } from '../services';
import OrderStatusBadge from '../components/OrderStatusBadge';
import OrderItemsList from '../components/OrderItemsList';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import { formatPrice } from '../../../shared/utils/formatters';
import { normalizeMarketplace } from '../../../shared/utils/marketplace';
import { useCurrency } from '../../currency/context/CurrencyContext';
import { ArrowLeft, MapPin, Phone, FileText, DollarSign, Clock, CreditCard, TrendingUp } from 'lucide-react';

const OrderDetailPage = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { toVND } = useCurrency();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [paying, setPaying] = useState(false);

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

  const handlePayRemaining = async () => {
    if (!order.remainingAmount) {
      toast.error('No remaining amount to pay');
      return;
    }

    if (!window.confirm(`Pay ${formatVND(order.remainingAmount)} from wallet?`)) {
      return;
    }

    try {
      setPaying(true);
      await payRemainingAmount(orderId);
      toast.success('Remaining amount paid successfully!');
      fetchOrder(); // Refresh order data
    } catch (err) {
      console.error('Error paying remaining amount:', err);
      toast.error(err.response?.data?.message || 'Failed to pay remaining amount');
    } finally {
      setPaying(false);
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

  // Format VND currency
  const formatVND = (amount) => {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  // Get payment status badge
  const getPaymentStatusBadge = (status) => {
    const statusConfig = {
      PENDING_DEPOSIT: { color: 'bg-yellow-100 text-yellow-800', label: 'Pending Deposit' },
      DEPOSITED: { color: 'bg-blue-100 text-blue-800', label: 'Deposit Paid' },
      PENDING_REMAINING: { color: 'bg-orange-100 text-orange-800', label: 'Awaiting Remaining' },
      WALLET_PAID: { color: 'bg-green-100 text-green-800', label: 'Fully Paid' },
      FULLY_COMPLETED: { color: 'bg-green-100 text-green-800', label: 'Completed' },
    };

    const config = statusConfig[status] || { color: 'bg-gray-100 text-gray-800', label: status };
    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`}>
        {config.label}
      </span>
    );
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

  // Calculate total items
  const totalItems = order.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0;
  const canCancel = order.status === 'PENDING';

  // Calculate total in VND (convert all currencies to VND)
  const totalVND = order.items?.reduce((sum, item) => {
    const currency = item.currency || 'VND';
    const price = item.price || 0;
    const quantity = item.quantity || 0;
    const itemTotal = price * quantity;

    // Convert to VND using exchange rates
    const itemTotalVND = toVND(itemTotal, currency);
    return sum + (itemTotalVND || 0);
  }, 0) || order.totalAmount || 0;

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

          {/* Payment Information */}
          {order.paymentStatus && (
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <h2 className="text-xl font-semibold mb-4">Payment Information</h2>

              <div className="space-y-4">
                {/* Payment Status */}
                <div className="flex items-center justify-between">
                  <span className="text-gray-600">Payment Status:</span>
                  {getPaymentStatusBadge(order.paymentStatus)}
                </div>

                {/* Deposit Amount */}
                {order.depositAmount && (
                  <div className="flex items-start gap-3 p-3 bg-blue-50 rounded-lg">
                    <CreditCard className="w-5 h-5 text-blue-600 mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-blue-600 font-medium mb-1">Deposit (70%)</p>
                      <p className="text-lg font-bold text-blue-900">{formatVND(order.depositAmount)}</p>
                      <p className="text-xs text-blue-600 mt-1">Already paid from wallet</p>
                    </div>
                  </div>
                )}

                {/* Remaining Amount */}
                {order.remainingAmount && (
                  <div className="space-y-2">
                    <div className="flex items-start gap-3 p-3 bg-orange-50 rounded-lg">
                      <DollarSign className="w-5 h-5 text-orange-600 mt-1" />
                      <div className="flex-1">
                        <p className="text-sm text-orange-600 font-medium mb-1">Remaining (30% + Fees)</p>
                        <p className="text-lg font-bold text-orange-900">{formatVND(order.remainingAmount)}</p>
                        <p className="text-xs text-orange-600 mt-1">
                          {order.paymentStatus === 'WALLET_PAID' || order.paymentStatus === 'FULLY_COMPLETED'
                            ? 'Already paid'
                            : 'To be paid when goods arrive'}
                        </p>
                      </div>
                    </div>

                    {/* Pay Remaining Button */}
                    {(order.paymentStatus === 'DEPOSITED' || order.paymentStatus === 'PENDING_REMAINING') && (
                      <button
                        onClick={handlePayRemaining}
                        disabled={paying}
                        className="w-full px-4 py-2 bg-orange-600 text-white rounded-lg font-medium hover:bg-orange-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {paying ? 'Processing...' : `Pay Remaining ${formatVND(order.remainingAmount)}`}
                      </button>
                    )}
                  </div>
                )}

                {/* Total Wallet Payment */}
                {order.totalAmount && (
                  <div className="flex items-center justify-between pt-3 border-t border-gray-200">
                    <span className="text-gray-900 font-semibold">Total (From Wallet):</span>
                    <span className="text-xl font-bold text-green-600">{formatVND(order.totalAmount)}</span>
                  </div>
                )}

                {/* COD Shipping Fee */}
                {order.vietnamDomesticShippingFee && order.isCodShipping && (
                  <div className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                    <div className="flex items-start gap-2">
                      <TrendingUp className="w-4 h-4 text-yellow-600 mt-0.5" />
                      <div className="flex-1">
                        <p className="text-sm font-medium text-yellow-800">COD Shipping Fee</p>
                        <p className="text-lg font-bold text-yellow-900">{formatVND(order.vietnamDomesticShippingFee)}</p>
                        <p className="text-xs text-yellow-600 mt-1">Pay to delivery person when receiving goods</p>
                      </div>
                    </div>
                  </div>
                )}

                {/* Fee Breakdown */}
                <div className="pt-3 border-t border-gray-200">
                  <p className="text-sm font-medium text-gray-700 mb-3">Tổng dự kiến chi tiết:</p>
                  <div className="space-y-2 text-sm">
                    {/* 1. Tiền hàng */}
                    <div className="flex justify-between text-gray-600">
                      <span>1. Tiền hàng:</span>
                      <span className="font-medium">
                        {order.productCost ? formatVND(order.productCost) : formatVND(totalVND)}
                      </span>
                    </div>

                    {/* 2. Phí mua hàng */}
                    <div className="flex justify-between text-gray-600">
                      <span>2. Phí mua hàng (1.5%):</span>
                      <span className="font-medium">
                        {order.serviceFee ? formatVND(order.serviceFee) : <span className="text-gray-400 italic">cập nhật</span>}
                      </span>
                    </div>

                    {/* 3. Phí vận chuyển nội địa TQ */}
                    <div className="flex justify-between text-gray-600">
                      <span>3. Phí vận chuyển nội địa TQ:</span>
                      <span className="font-medium">
                        {order.domesticShippingFee ? formatVND(order.domesticShippingFee) : <span className="text-gray-400 italic">cập nhật</span>}
                      </span>
                    </div>

                    {/* 4. Phí vận chuyển quốc tế */}
                    <div className="flex justify-between text-gray-600">
                      <span>4. Phí vận chuyển quốc tế TQ - VN:</span>
                      <span className="font-medium">
                        {order.internationalShippingFee ? formatVND(order.internationalShippingFee) : <span className="text-gray-400 italic">cập nhật</span>}
                      </span>
                    </div>

                    {/* 5. Phí dịch vụ bổ sung */}
                    <div className="flex justify-between text-gray-600">
                      <span>5. Phí dịch vụ (đóng gỗ, bọt khí, kiểm đếm):</span>
                      <span className="font-medium">
                        {order.additionalServicesFee ? formatVND(order.additionalServicesFee) : <span className="text-gray-400 italic">cập nhật</span>}
                      </span>
                    </div>

                    {/* Total line */}
                    <div className="flex justify-between text-gray-900 font-semibold pt-2 border-t border-gray-200">
                      <span>Tổng thanh toán từ ví:</span>
                      <span className="text-lg">{formatVND(order.totalAmount)}</span>
                    </div>

                    {/* Deposit breakdown */}
                    <div className="flex justify-between text-blue-600 text-xs pt-2">
                      <span>→ Tiền cọc (70%):</span>
                      <span className="font-medium">{order.depositAmount ? formatVND(order.depositAmount) : '-'}</span>
                    </div>

                    <div className="flex justify-between text-orange-600 text-xs">
                      <span>→ Còn lại (30% + phí):</span>
                      <span className="font-medium">{order.remainingAmount ? formatVND(order.remainingAmount) : '-'}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Status History Timeline */}
          {order.statusHistory && order.statusHistory.length > 0 && (
            <div className="bg-white rounded-lg border border-gray-200 p-6">
              <h2 className="text-xl font-semibold mb-4">Order Status History</h2>

              <div className="space-y-4">
                {order.statusHistory.map((history, index) => (
                  <div key={history.id || index} className="flex gap-4">
                    {/* Timeline Dot */}
                    <div className="flex flex-col items-center">
                      <div className={`w-3 h-3 rounded-full ${
                        index === 0 ? 'bg-green-500' : 'bg-gray-300'
                      }`} />
                      {index < order.statusHistory.length - 1 && (
                        <div className="w-0.5 h-full bg-gray-200 my-1" />
                      )}
                    </div>

                    {/* Timeline Content */}
                    <div className="flex-1 pb-4">
                      <div className="flex items-center gap-2 mb-1">
                        <OrderStatusBadge status={history.status} />
                        {history.previousStatus && (
                          <span className="text-xs text-gray-500">
                            (from {history.previousStatus})
                          </span>
                        )}
                      </div>

                      {history.note && (
                        <p className="text-sm text-gray-600 mb-1">{history.note}</p>
                      )}

                      <div className="flex items-center gap-2 text-xs text-gray-500">
                        <Clock className="w-3 h-3" />
                        <span>{formatDate(history.createdAt)}</span>
                        {history.changedBy && (
                          <span className="ml-2">• Changed by Admin (ID: {history.changedBy})</span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
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
                <div className="flex justify-between items-baseline">
                  <span className="text-gray-600">Tiền hàng:</span>
                  <span className="text-2xl font-bold text-red-600">
                    {formatVND(totalVND)}
                  </span>
                </div>
              </div>
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
