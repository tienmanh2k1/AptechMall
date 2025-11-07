import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { createAddress, updateAddress } from '../services/addressApi';
import { X, MapPin, User, Phone, Home, Briefcase, MapPinned } from 'lucide-react';

/**
 * Add/Edit Address Modal
 * Modal for creating or editing user addresses
 */
const AddAddressModal = ({ isOpen, onClose, onSuccess, address }) => {
  const isEditMode = !!address;

  const [formData, setFormData] = useState({
    receiverName: '',
    phone: '',
    province: '',
    district: '',
    ward: '',
    addressDetail: '',
    addressType: 'HOME',
    isDefault: false,
  });
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (address) {
      setFormData({
        receiverName: address.receiverName || '',
        phone: address.phone || '',
        province: address.province || '',
        district: address.district || '',
        ward: address.ward || '',
        addressDetail: address.addressDetail || '',
        addressType: address.addressType || 'HOME',
        isDefault: address.isDefault || false,
      });
    } else {
      setFormData({
        receiverName: '',
        phone: '',
        province: '',
        district: '',
        ward: '',
        addressDetail: '',
        addressType: 'HOME',
        isDefault: false,
      });
    }
    setErrors({});
  }, [address, isOpen]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.receiverName.trim()) {
      newErrors.receiverName = 'Vui lòng nhập tên người nhận';
    }

    if (!formData.phone.trim()) {
      newErrors.phone = 'Vui lòng nhập số điện thoại';
    } else if (!/^[0-9]{10,11}$/.test(formData.phone)) {
      newErrors.phone = 'Số điện thoại phải có 10-11 chữ số';
    }

    if (!formData.province.trim()) {
      newErrors.province = 'Vui lòng nhập tỉnh/thành phố';
    }

    if (!formData.district.trim()) {
      newErrors.district = 'Vui lòng nhập quận/huyện';
    }

    if (!formData.ward.trim()) {
      newErrors.ward = 'Vui lòng nhập phường/xã';
    }

    if (!formData.addressDetail.trim()) {
      newErrors.addressDetail = 'Vui lòng nhập địa chỉ chi tiết';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);
      if (isEditMode) {
        await updateAddress(address.id, formData);
        toast.success('Cập nhật địa chỉ thành công!');
      } else {
        await createAddress(formData);
        toast.success('Thêm địa chỉ thành công!');
      }
      handleClose();
      if (onSuccess) {
        onSuccess();
      }
    } catch (error) {
      console.error('Error saving address:', error);
      const errorMessage =
        error.response?.data?.message ||
        (isEditMode ? 'Cập nhật địa chỉ thất bại' : 'Thêm địa chỉ thất bại');
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFormData({
      receiverName: '',
      phone: '',
      province: '',
      district: '',
      ward: '',
      addressDetail: '',
      addressType: 'HOME',
      isDefault: false,
    });
    setErrors({});
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Overlay */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
        onClick={handleClose}
      />

      {/* Modal */}
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
            <div className="flex items-center">
              <MapPin className="w-5 h-5 text-primary-600 mr-2" />
              <h3 className="text-lg font-semibold text-gray-900">
                {isEditMode ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới'}
              </h3>
            </div>
            <button
              onClick={handleClose}
              className="text-gray-400 hover:text-gray-500 transition-colors"
              disabled={loading}
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-6">
            <div className="space-y-4">
              {/* Receiver Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  <User className="w-4 h-4 inline mr-1" />
                  Tên người nhận <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="receiverName"
                  value={formData.receiverName}
                  onChange={handleInputChange}
                  className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                    errors.receiverName ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Nhập tên người nhận"
                  disabled={loading}
                />
                {errors.receiverName && (
                  <p className="text-red-500 text-xs mt-1">{errors.receiverName}</p>
                )}
              </div>

              {/* Phone */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  <Phone className="w-4 h-4 inline mr-1" />
                  Số điện thoại <span className="text-red-500">*</span>
                </label>
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleInputChange}
                  className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                    errors.phone ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Nhập số điện thoại (10-11 chữ số)"
                  disabled={loading}
                />
                {errors.phone && (
                  <p className="text-red-500 text-xs mt-1">{errors.phone}</p>
                )}
              </div>

              {/* Province, District, Ward */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Tỉnh/Thành phố <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="province"
                    value={formData.province}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                      errors.province ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Tỉnh/TP"
                    disabled={loading}
                  />
                  {errors.province && (
                    <p className="text-red-500 text-xs mt-1">{errors.province}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Quận/Huyện <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="district"
                    value={formData.district}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                      errors.district ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Quận/Huyện"
                    disabled={loading}
                  />
                  {errors.district && (
                    <p className="text-red-500 text-xs mt-1">{errors.district}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Phường/Xã <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="ward"
                    value={formData.ward}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                      errors.ward ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Phường/Xã"
                    disabled={loading}
                  />
                  {errors.ward && (
                    <p className="text-red-500 text-xs mt-1">{errors.ward}</p>
                  )}
                </div>
              </div>

              {/* Address Detail */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Địa chỉ chi tiết <span className="text-red-500">*</span>
                </label>
                <textarea
                  name="addressDetail"
                  value={formData.addressDetail}
                  onChange={handleInputChange}
                  rows="3"
                  className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                    errors.addressDetail ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Số nhà, tên đường..."
                  disabled={loading}
                />
                {errors.addressDetail && (
                  <p className="text-red-500 text-xs mt-1">{errors.addressDetail}</p>
                )}
              </div>

              {/* Address Type */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Loại địa chỉ
                </label>
                <div className="grid grid-cols-3 gap-3">
                  <button
                    type="button"
                    onClick={() =>
                      handleInputChange({
                        target: { name: 'addressType', value: 'HOME' },
                      })
                    }
                    className={`flex items-center justify-center gap-2 px-4 py-2 border rounded-lg transition-colors ${
                      formData.addressType === 'HOME'
                        ? 'border-primary-500 bg-primary-50 text-primary-700'
                        : 'border-gray-300 text-gray-700 hover:border-gray-400'
                    }`}
                    disabled={loading}
                  >
                    <Home className="w-4 h-4" />
                    Nhà riêng
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      handleInputChange({
                        target: { name: 'addressType', value: 'OFFICE' },
                      })
                    }
                    className={`flex items-center justify-center gap-2 px-4 py-2 border rounded-lg transition-colors ${
                      formData.addressType === 'OFFICE'
                        ? 'border-primary-500 bg-primary-50 text-primary-700'
                        : 'border-gray-300 text-gray-700 hover:border-gray-400'
                    }`}
                    disabled={loading}
                  >
                    <Briefcase className="w-4 h-4" />
                    Văn phòng
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      handleInputChange({
                        target: { name: 'addressType', value: 'OTHER' },
                      })
                    }
                    className={`flex items-center justify-center gap-2 px-4 py-2 border rounded-lg transition-colors ${
                      formData.addressType === 'OTHER'
                        ? 'border-primary-500 bg-primary-50 text-primary-700'
                        : 'border-gray-300 text-gray-700 hover:border-gray-400'
                    }`}
                    disabled={loading}
                  >
                    <MapPinned className="w-4 h-4" />
                    Khác
                  </button>
                </div>
              </div>

              {/* Is Default */}
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="isDefault"
                  name="isDefault"
                  checked={formData.isDefault}
                  onChange={handleInputChange}
                  className="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                  disabled={loading}
                />
                <label
                  htmlFor="isDefault"
                  className="ml-2 text-sm text-gray-700"
                >
                  Đặt làm địa chỉ mặc định
                </label>
              </div>
            </div>

            {/* Actions */}
            <div className="flex gap-3 mt-6">
              <button
                type="button"
                onClick={handleClose}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                disabled={loading}
              >
                Hủy
              </button>
              <button
                type="submit"
                className="flex-1 px-4 py-2 border border-transparent rounded-lg text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
                disabled={loading}
              >
                {loading
                  ? 'Đang xử lý...'
                  : isEditMode
                  ? 'Cập nhật'
                  : 'Thêm địa chỉ'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default AddAddressModal;
