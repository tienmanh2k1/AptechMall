import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import {
  getOrderById,
  updateOrderStatus,
  updateOrderFees,
  updateOrderAddress,
} from '../services/adminOrderApi';
import OrderStatusBadge from '../../order/components/OrderStatusBadge';
import OrderItemsList from '../../order/components/OrderItemsList';
import UpdateOrderFeesModal from '../components/UpdateOrderFeesModal';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import {
  ArrowLeft,
  User,
  MapPin,
  Phone,
  Mail,
  FileText,
  DollarSign,
  Package,
  Truck,
  Edit,
  Clock,
  CreditCard,
  ShoppingCart,
} from 'lucide-react';

/**
 * Admin Order Detail Page
 * Shows complete order information for admin/staff
 */
const AdminOrderDetailPage = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal states
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [showFeesModal, setShowFeesModal] = useState(false);
  const [newStatus, setNewStatus] = useState('');
  const [statusNote, setStatusNote] = useState('');
  const [updatingStatus, setUpdatingStatus] = useState(false);

  // Update address modal state
  const [showUpdateAddressModal, setShowUpdateAddressModal] = useState(false);
  const [updatingAddress, setUpdatingAddress] = useState(false);
  const [addressFormData, setAddressFormData] = useState({
    shippingAddress: '',
    phone: '',
    note: ''
  });

  const fetchOrder = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await getOrderById(orderId);
      if (response.success && response.data) {
        setOrder(response.data);
        setNewStatus(response.data.status);
      } else {
        setError('Failed to load order');
      }
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

  const handleUpdateStatus = async () => {
    if (!newStatus) {
      toast.error('Please select a status');
      return;
    }

    if (newStatus === order.status) {
      toast.info('Status is already set to ' + newStatus);
      setShowStatusModal(false);
      return;
    }

    try {
      setUpdatingStatus(true);
      const response = await updateOrderStatus(order.id, newStatus, statusNote || null);

      if (response.success) {
        toast.success(`Order status updated to ${newStatus}`);
        setShowStatusModal(false);
        fetchOrder(); // Refresh order data
      } else {
        toast.error(response.message || 'Failed to update status');
      }
    } catch (error) {
      console.error('Error updating status:', error);
      toast.error(error.response?.data?.message || 'Failed to update order status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleUpdateFees = async (orderId, feesData) => {
    try {
      const response = await updateOrderFees(orderId, feesData);

      if (response.success) {
        toast.success('Đã cập nhật phí đơn hàng thành công!');
        fetchOrder(); // Refresh order data
      } else {
        toast.error(response.message || 'Cập nhật phí thất bại');
      }
    } catch (error) {
      console.error('Error updating fees:', error);
      throw error; // Let the modal handle the error
    }
  };

  const handleOpenUpdateAddressModal = () => {
    setAddressFormData({
      shippingAddress: order.shippingAddress || '',
      phone: order.phone || '',
      note: ''
    });
    setShowUpdateAddressModal(true);
  };

  const handleUpdateAddress = async () => {
    if (!addressFormData.shippingAddress.trim() || !addressFormData.phone.trim()) {
      toast.error('Vui lòng nhập đầy đủ địa chỉ và số điện thoại');
      return;
    }

    try {
      setUpdatingAddress(true);
      const response = await updateOrderAddress(order.id, addressFormData);

      if (response.success) {
        toast.success('Đã cập nhật địa chỉ đơn hàng thành công!');
        setShowUpdateAddressModal(false);
        fetchOrder(); // Refresh order data
      } else {
        toast.error(response.message || 'Cập nhật địa chỉ thất bại');
      }
    } catch (error) {
      console.error('Error updating order address:', error);
      toast.error(error.response?.data?.message || 'Cập nhật địa chỉ thất bại');
    } finally {
      setUpdatingAddress(false);
    }
  };

  const formatCurrency = (amount) => {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateString;
    }
  };

  if (loading) {
    return <Loading message="Đang tải thông tin đơn hàng..." />;
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <ErrorMessage message={error} onRetry={fetchOrder} />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <ErrorMessage message="Không tìm thấy đơn hàng" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={() => navigate('/admin/orders')}
            className="flex items-center text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            Quay lại danh sách đơn hàng
          </button>

          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                Chi tiết đơn hàng #{order.orderNumber}
              </h1>
              <p className="text-gray-600 mt-1">Order ID: {order.id}</p>
            </div>

            <div className="flex items-center space-x-3">
              <OrderStatusBadge status={order.status} />
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="mb-6 flex flex-col gap-3">
          <div className="flex gap-3">
            <button
              onClick={() => setShowStatusModal(true)}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <Edit className="w-4 h-4 mr-2" />
              Cập nhật trạng thái
            </button>
            {order.status === 'CONFIRMED' ? (
              <button
                onClick={() => setShowFeesModal(true)}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
              >
                <Truck className="w-4 h-4 mr-2" />
                Cập nhật phí
              </button>
            ) : (
              <button
                disabled
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-gray-400 cursor-not-allowed"
                title="Chỉ có thể cập nhật phí khi đơn hàng đã CONFIRMED"
              >
                <Truck className="w-4 h-4 mr-2" />
                Cập nhật phí
              </button>
            )}
          </div>
          {order.status !== 'CONFIRMED' && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-sm text-yellow-800">
              <strong>Lưu ý:</strong> Chỉ có thể cập nhật phí khi đơn hàng ở trạng thái <strong>CONFIRMED</strong>.
              Vui lòng xác nhận đơn hàng trước.
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content - Left Column */}
          <div className="lg:col-span-2 space-y-6">
            {/* Customer Information */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <User className="w-5 h-5 mr-2 text-blue-600" />
                  Thông tin khách hàng
                </h2>
                {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
                  <button
                    onClick={handleOpenUpdateAddressModal}
                    className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-blue-600 hover:text-blue-700 border border-blue-300 rounded-lg hover:bg-blue-50 transition-colors"
                  >
                    <Edit className="w-4 h-4 mr-1" />
                    Thay đổi địa chỉ
                  </button>
                )}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-600">User ID</p>
                  <p className="text-base font-medium text-gray-900">
                    {order.userId}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-600 flex items-center">
                    <Phone className="w-4 h-4 mr-1" />
                    Số điện thoại
                  </p>
                  <p className="text-base font-medium text-gray-900">
                    {order.phone || 'N/A'}
                  </p>
                </div>
                <div className="md:col-span-2">
                  <p className="text-sm text-gray-600 flex items-center">
                    <MapPin className="w-4 h-4 mr-1" />
                    Địa chỉ giao hàng
                  </p>
                  <p className="text-base font-medium text-gray-900">
                    {order.shippingAddress || 'N/A'}
                  </p>
                </div>
              </div>
              {order.status !== 'PENDING' && order.status !== 'CONFIRMED' && (
                <div className="mt-3 text-xs text-gray-500 bg-gray-50 p-2 rounded">
                  ℹ️ Chỉ có thể thay đổi địa chỉ khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED
                </div>
              )}
            </div>

            {/* Order Items */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <ShoppingCart className="w-5 h-5 mr-2 text-blue-600" />
                Sản phẩm trong đơn hàng ({order.totalItems} sản phẩm)
              </h2>
              <OrderItemsList items={order.items || []} />
            </div>

            {/* Note */}
            {order.note && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <FileText className="w-5 h-5 mr-2 text-blue-600" />
                  Ghi chú
                </h2>
                <p className="text-gray-700 whitespace-pre-wrap">{order.note}</p>
              </div>
            )}
          </div>

          {/* Right Column - Order Summary */}
          <div className="space-y-6">
            {/* Payment Status */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <CreditCard className="w-5 h-5 mr-2 text-blue-600" />
                Trạng thái thanh toán
              </h2>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-gray-600">Trạng thái:</span>
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-medium ${
                      order.paymentStatus === 'PAID'
                        ? 'bg-green-100 text-green-800'
                        : order.paymentStatus === 'PARTIALLY_PAID'
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {order.paymentStatus === 'PAID'
                      ? 'Đã thanh toán'
                      : order.paymentStatus === 'PARTIALLY_PAID'
                      ? 'Đã cọc'
                      : 'Chưa thanh toán'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Đã cọc:</span>
                  <span className="font-semibold">
                    {formatCurrency(order.depositAmount)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Còn lại:</span>
                  <span className="font-semibold text-orange-600">
                    {formatCurrency(order.remainingAmount)}
                  </span>
                </div>
              </div>
            </div>

            {/* Fee Breakdown */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <DollarSign className="w-5 h-5 mr-2 text-blue-600" />
                Chi tiết phí
              </h2>
              <div className="space-y-3">
                {/* Product Cost */}
                <div className="flex justify-between">
                  <span className="text-gray-600">Tiền hàng:</span>
                  <span className="font-medium">
                    {formatCurrency(order.productCost)}
                  </span>
                </div>

                {/* Service Fee */}
                <div className="flex justify-between">
                  <span className="text-gray-600">Phí dịch vụ (1.5%):</span>
                  <span className="font-medium">
                    {formatCurrency(order.serviceFee)}
                  </span>
                </div>

                <div className="border-t pt-3 mt-3">
                  <p className="text-sm font-medium text-gray-700 mb-2">
                    Phí vận chuyển:
                  </p>

                  {/* Domestic Shipping */}
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">- Nội địa TQ:</span>
                    <span>
                      {order.domesticShippingFee
                        ? formatCurrency(order.domesticShippingFee)
                        : 'Chưa cập nhật'}
                    </span>
                  </div>

                  {/* International Shipping */}
                  <div className="flex justify-between text-sm mt-1">
                    <span className="text-gray-600">- Quốc tế:</span>
                    <span>
                      {order.internationalShippingFee
                        ? formatCurrency(order.internationalShippingFee)
                        : 'Chưa cập nhật'}
                    </span>
                  </div>

                  {/* Vietnam Domestic (COD) */}
                  {order.vietnamDomesticShippingFee > 0 && (
                    <div className="flex justify-between text-sm mt-1">
                      <span className="text-gray-600">- Nội địa VN (COD):</span>
                      <span>{formatCurrency(order.vietnamDomesticShippingFee)}</span>
                    </div>
                  )}
                </div>

                {/* Additional Services */}
                {order.additionalServicesFee > 0 && (
                  <div className="border-t pt-3">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Phí dịch vụ bổ sung:</span>
                      <span className="font-medium">
                        {formatCurrency(order.additionalServicesFee)}
                      </span>
                    </div>
                    {order.estimatedWeight && (
                      <p className="text-xs text-gray-500 mt-1">
                        Cân nặng: {order.estimatedWeight} kg
                      </p>
                    )}
                  </div>
                )}

                {/* Total */}
                <div className="border-t-2 pt-3 mt-3">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-semibold text-gray-900">
                      Tổng cộng:
                    </span>
                    <span className="text-xl font-bold text-red-600">
                      {formatCurrency(order.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Order Timeline */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <Clock className="w-5 h-5 mr-2 text-blue-600" />
                Thời gian
              </h2>
              <div className="space-y-3 text-sm">
                <div>
                  <p className="text-gray-600">Ngày tạo đơn:</p>
                  <p className="font-medium text-gray-900">
                    {formatDate(order.createdAt)}
                  </p>
                </div>
                {order.updatedAt && order.updatedAt !== order.createdAt && (
                  <div>
                    <p className="text-gray-600">Cập nhật lần cuối:</p>
                    <p className="font-medium text-gray-900">
                      {formatDate(order.updatedAt)}
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Update Status Modal */}
      {showStatusModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Cập nhật trạng thái đơn hàng
            </h3>

            <div className="mb-4">
              <p className="text-sm text-gray-600 mb-2">
                Đơn hàng: <span className="font-semibold">{order.orderNumber}</span>
              </p>
              <p className="text-sm text-gray-600">
                Trạng thái hiện tại: <OrderStatusBadge status={order.status} />
              </p>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Trạng thái mới *
              </label>
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="PENDING">Pending</option>
                <option value="CONFIRMED">Confirmed</option>
                <option value="SHIPPING">Shipping</option>
                <option value="DELIVERED">Delivered</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Ghi chú (Optional)
              </label>
              <textarea
                value={statusNote}
                onChange={(e) => setStatusNote(e.target.value)}
                rows="3"
                placeholder="Thêm ghi chú..."
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowStatusModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                disabled={updatingStatus}
              >
                Hủy
              </button>
              <button
                onClick={handleUpdateStatus}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                disabled={updatingStatus}
              >
                {updatingStatus ? 'Đang cập nhật...' : 'Cập nhật'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Update Fees Modal */}
      <UpdateOrderFeesModal
        isOpen={showFeesModal}
        onClose={() => setShowFeesModal(false)}
        order={order}
        onUpdate={handleUpdateFees}
      />

      {/* Update Address Modal */}
      {showUpdateAddressModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Thay đổi địa chỉ giao hàng
            </h3>

            <div className="space-y-4">
              {/* Shipping Address */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Địa chỉ giao hàng *
                </label>
                <textarea
                  value={addressFormData.shippingAddress}
                  onChange={(e) => setAddressFormData({ ...addressFormData, shippingAddress: e.target.value })}
                  rows="3"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Nhập địa chỉ chi tiết..."
                />
              </div>

              {/* Phone */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Số điện thoại *
                </label>
                <input
                  type="tel"
                  value={addressFormData.phone}
                  onChange={(e) => setAddressFormData({ ...addressFormData, phone: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Nhập số điện thoại..."
                />
              </div>

              {/* Note */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Ghi chú (Không bắt buộc)
                </label>
                <textarea
                  value={addressFormData.note}
                  onChange={(e) => setAddressFormData({ ...addressFormData, note: e.target.value })}
                  rows="2"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Lý do thay đổi địa chỉ..."
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowUpdateAddressModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                disabled={updatingAddress}
              >
                Hủy
              </button>
              <button
                onClick={handleUpdateAddress}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                disabled={updatingAddress}
              >
                {updatingAddress ? 'Đang cập nhật...' : 'Cập nhật'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminOrderDetailPage;
