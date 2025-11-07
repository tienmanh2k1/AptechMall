import { useState, useEffect } from 'react';
import { X } from 'lucide-react';

/**
 * Modal to update order fees
 * Admin/Staff can update shipping fees, weight, and additional services
 */
const UpdateOrderFeesModal = ({ isOpen, onClose, order, onUpdate }) => {
  const [formData, setFormData] = useState({
    domesticShippingFee: '',
    internationalShippingFee: '',
    estimatedWeight: '',
    includeWoodenPackaging: false,
    includeBubbleWrap: false,
    includeItemCountCheck: false,
    note: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Initialize form with order data
  useEffect(() => {
    if (order) {
      setFormData({
        domesticShippingFee: order.domesticShippingFee || '',
        internationalShippingFee: order.internationalShippingFee || '',
        estimatedWeight: order.estimatedWeight || '',
        includeWoodenPackaging: false,
        includeBubbleWrap: false,
        includeItemCountCheck: false,
        note: '',
      });
    }
  }, [order]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Convert string values to numbers
      const feesData = {
        domesticShippingFee: formData.domesticShippingFee
          ? parseFloat(formData.domesticShippingFee)
          : null,
        internationalShippingFee: formData.internationalShippingFee
          ? parseFloat(formData.internationalShippingFee)
          : null,
        estimatedWeight: formData.estimatedWeight
          ? parseFloat(formData.estimatedWeight)
          : null,
        includeWoodenPackaging: formData.includeWoodenPackaging,
        includeBubbleWrap: formData.includeBubbleWrap,
        includeItemCountCheck: formData.includeItemCountCheck,
        note: formData.note || null,
      };

      await onUpdate(order.id, feesData);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Cập nhật phí thất bại');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <div>
            <h2 className="text-xl font-semibold text-gray-800">
              Cập nhật phí đơn hàng
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              Mã đơn: #{order?.id} - {order?.orderNumber}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {/* Shipping Fees Section */}
          <div className="space-y-4">
            <h3 className="font-semibold text-gray-700 text-lg">
              Phí vận chuyển
            </h3>

            {/* Domestic Shipping Fee */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Phí vận chuyển nội địa TQ (CNY)
              </label>
              <input
                type="number"
                name="domesticShippingFee"
                value={formData.domesticShippingFee}
                onChange={handleChange}
                step="0.01"
                min="0"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                placeholder="Nhập phí vận chuyển nội địa TQ"
              />
              <p className="text-xs text-gray-500 mt-1">
                Phí vận chuyển từ nhà cung cấp đến kho TQ (tính bằng CNY)
              </p>
            </div>

            {/* International Shipping Fee */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Phí vận chuyển quốc tế (VND)
              </label>
              <input
                type="number"
                name="internationalShippingFee"
                value={formData.internationalShippingFee}
                onChange={handleChange}
                step="0.01"
                min="0"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                placeholder="Nhập phí vận chuyển quốc tế"
              />
              <p className="text-xs text-gray-500 mt-1">
                Phí vận chuyển từ TQ về VN (tính bằng VND)
              </p>
            </div>

            {/* Estimated Weight */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Cân nặng ước tính (kg)
              </label>
              <input
                type="number"
                name="estimatedWeight"
                value={formData.estimatedWeight}
                onChange={handleChange}
                step="0.1"
                min="0"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                placeholder="Nhập cân nặng"
              />
              <p className="text-xs text-gray-500 mt-1">
                Cân nặng ước tính để tính phí vận chuyển
              </p>
            </div>
          </div>

          {/* Additional Services Section */}
          <div className="space-y-4">
            <h3 className="font-semibold text-gray-700 text-lg">
              Dịch vụ bổ sung
            </h3>

            <div className="space-y-3">
              {/* Wooden Packaging */}
              <label className="flex items-start space-x-3 cursor-pointer group">
                <input
                  type="checkbox"
                  name="includeWoodenPackaging"
                  checked={formData.includeWoodenPackaging}
                  onChange={handleChange}
                  className="mt-1 w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <div className="flex-1">
                  <span className="text-sm font-medium text-gray-700 group-hover:text-gray-900">
                    Phí đóng gỗ
                  </span>
                  <p className="text-xs text-gray-500">
                    Kg đầu tiên: 20 tệ | Kg tiếp theo: 1 tệ/kg
                  </p>
                </div>
              </label>

              {/* Bubble Wrap */}
              <label className="flex items-start space-x-3 cursor-pointer group">
                <input
                  type="checkbox"
                  name="includeBubbleWrap"
                  checked={formData.includeBubbleWrap}
                  onChange={handleChange}
                  className="mt-1 w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <div className="flex-1">
                  <span className="text-sm font-medium text-gray-700 group-hover:text-gray-900">
                    Phí đóng bọt khí
                  </span>
                  <p className="text-xs text-gray-500">
                    Kg đầu tiên: 10 tệ | Kg tiếp theo: 1.5 tệ/kg
                  </p>
                </div>
              </label>

              {/* Item Count Check */}
              <label className="flex items-start space-x-3 cursor-pointer group">
                <input
                  type="checkbox"
                  name="includeItemCountCheck"
                  checked={formData.includeItemCountCheck}
                  onChange={handleChange}
                  className="mt-1 w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                />
                <div className="flex-1">
                  <span className="text-sm font-medium text-gray-700 group-hover:text-gray-900">
                    Phí kiểm đếm
                  </span>
                  <p className="text-xs text-gray-500">
                    Tính theo số lượng sản phẩm: 800-5,000đ/SP
                    <br />
                    (SP phụ kiện {'<'}10 tệ: 800-2,500đ/SP)
                  </p>
                </div>
              </label>
            </div>

            {/* Pricing info box */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p className="text-xs text-blue-800 font-medium mb-1">
                ℹ️ Cách tính phí:
              </p>
              <ul className="text-xs text-blue-700 space-y-1">
                <li>
                  • <strong>Phí kiểm đếm:</strong> Tự động tính dựa trên số
                  lượng SP trong đơn
                </li>
                <li>
                  • <strong>Phí đóng gỗ/bọt khí:</strong> Tính theo cân nặng
                  (CNY → VND)
                </li>
                <li>
                  • Hệ thống sẽ tự động convert tệ sang VND theo tỷ giá
                </li>
              </ul>
            </div>
          </div>

          {/* Note */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Ghi chú
            </label>
            <textarea
              name="note"
              value={formData.note}
              onChange={handleChange}
              rows="3"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="Nhập ghi chú về việc cập nhật phí..."
            />
          </div>

          {/* Current Order Info */}
          <div className="bg-gray-50 p-4 rounded-lg space-y-2">
            <h4 className="font-medium text-gray-700 text-sm">
              Thông tin hiện tại:
            </h4>
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div>
                <span className="text-gray-600">Phí nội địa TQ:</span>
                <span className="ml-2 font-medium">
                  {order?.domesticShippingFee
                    ? `${order.domesticShippingFee.toLocaleString()} CNY`
                    : 'Chưa cập nhật'}
                </span>
              </div>
              <div>
                <span className="text-gray-600">Phí quốc tế:</span>
                <span className="ml-2 font-medium">
                  {order?.internationalShippingFee
                    ? `${order.internationalShippingFee.toLocaleString()} VND`
                    : 'Chưa cập nhật'}
                </span>
              </div>
              <div>
                <span className="text-gray-600">Cân nặng:</span>
                <span className="ml-2 font-medium">
                  {order?.estimatedWeight
                    ? `${order.estimatedWeight} kg`
                    : 'Chưa cập nhật'}
                </span>
              </div>
              <div>
                <span className="text-gray-600">Phí dịch vụ bổ sung:</span>
                <span className="ml-2 font-medium">
                  {order?.additionalServicesFee
                    ? `${order.additionalServicesFee.toLocaleString()} VND`
                    : '0 VND'}
                </span>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3 pt-4 border-t">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              disabled={loading}
            >
              Hủy
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
              disabled={loading}
            >
              {loading ? 'Đang cập nhật...' : 'Cập nhật phí'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UpdateOrderFeesModal;
