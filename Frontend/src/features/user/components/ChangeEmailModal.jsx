import React, { useState } from 'react';
import { toast } from 'react-toastify';
import { updateCredentials } from '../../auth/services/authApi';
import { X, Mail, Lock, Eye, EyeOff } from 'lucide-react';

/**
 * Change Email Modal
 * Allows user to change their email address by providing new email and current password
 */
const ChangeEmailModal = ({ isOpen, onClose, currentEmail, onSuccess }) => {
  const [formData, setFormData] = useState({
    newEmail: '',
    currentPassword: '',
  });
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  const validateEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.newEmail) {
      newErrors.newEmail = 'Vui lòng nhập email mới';
    } else if (!validateEmail(formData.newEmail)) {
      newErrors.newEmail = 'Email không hợp lệ';
    } else if (formData.newEmail === currentEmail) {
      newErrors.newEmail = 'Email mới phải khác email hiện tại';
    }

    if (!formData.currentPassword) {
      newErrors.currentPassword = 'Vui lòng nhập mật khẩu để xác nhận';
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
      const response = await updateCredentials({
        email: formData.newEmail,
        currentPassword: formData.currentPassword,
      });

      if (response.success || response.message) {
        toast.success('Đổi email thành công!');
        handleClose();
        if (onSuccess) {
          onSuccess();
        }
      } else {
        toast.error('Đổi email thất bại');
      }
    } catch (error) {
      console.error('Error changing email:', error);
      const errorMessage = error.response?.data?.message || 'Đổi email thất bại';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFormData({
      newEmail: '',
      currentPassword: '',
    });
    setErrors({});
    setShowPassword(false);
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
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <div className="flex items-center">
              <Mail className="w-5 h-5 text-primary-600 mr-2" />
              <h3 className="text-lg font-semibold text-gray-900">
                Đổi địa chỉ email
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
              {/* Current Email (Read-only) */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email hiện tại
                </label>
                <input
                  type="email"
                  value={currentEmail || 'Chưa có email'}
                  disabled
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600 cursor-not-allowed"
                />
              </div>

              {/* New Email */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email mới <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type="email"
                    name="newEmail"
                    value={formData.newEmail}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                      errors.newEmail ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Nhập email mới"
                    disabled={loading}
                  />
                  <Mail className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                </div>
                {errors.newEmail && (
                  <p className="text-red-500 text-xs mt-1">{errors.newEmail}</p>
                )}
              </div>

              {/* Current Password */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Mật khẩu hiện tại <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="currentPassword"
                    value={formData.currentPassword}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-2 pr-10 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
                      errors.currentPassword
                        ? 'border-red-500'
                        : 'border-gray-300'
                    }`}
                    placeholder="Nhập mật khẩu để xác nhận"
                    disabled={loading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    tabIndex={-1}
                  >
                    {showPassword ? (
                      <EyeOff className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </button>
                </div>
                {errors.currentPassword && (
                  <p className="text-red-500 text-xs mt-1">
                    {errors.currentPassword}
                  </p>
                )}
                <p className="text-xs text-gray-500 mt-1">
                  Nhập mật khẩu hiện tại để xác nhận thay đổi
                </p>
              </div>
            </div>

            {/* Info Box */}
            <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-800">
                <strong>Lưu ý:</strong> Email mới sẽ được sử dụng để đăng nhập vào
                tài khoản của bạn.
              </p>
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
                {loading ? 'Đang xử lý...' : 'Đổi email'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ChangeEmailModal;
