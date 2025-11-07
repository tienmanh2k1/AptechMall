import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import {
  getAllAddresses,
  deleteAddress,
  setDefaultAddress,
} from '../services/addressApi';
import AddAddressModal from '../components/AddAddressModal';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import {
  MapPin,
  Plus,
  Edit,
  Trash2,
  Star,
  Phone,
  User,
  Home,
  Briefcase,
  MapPinned,
} from 'lucide-react';

/**
 * Address Management Page
 * Allows users to view, add, edit, and delete their saved addresses
 */
const AddressManagementPage = () => {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingAddress, setEditingAddress] = useState(null);

  const fetchAddresses = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllAddresses();
      setAddresses(data);
    } catch (err) {
      console.error('Error fetching addresses:', err);
      setError(err.response?.data?.message || 'Không thể tải danh sách địa chỉ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAddresses();
  }, []);

  const handleAddAddress = () => {
    setEditingAddress(null);
    setShowAddModal(true);
  };

  const handleEditAddress = (address) => {
    setEditingAddress(address);
    setShowAddModal(true);
  };

  const handleDeleteAddress = async (addressId) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) {
      return;
    }

    try {
      await deleteAddress(addressId);
      toast.success('Đã xóa địa chỉ');
      fetchAddresses();
    } catch (error) {
      console.error('Error deleting address:', error);
      toast.error(error.response?.data?.message || 'Xóa địa chỉ thất bại');
    }
  };

  const handleSetDefault = async (addressId) => {
    try {
      await setDefaultAddress(addressId);
      toast.success('Đã đặt làm địa chỉ mặc định');
      fetchAddresses();
    } catch (error) {
      console.error('Error setting default address:', error);
      toast.error(
        error.response?.data?.message || 'Đặt địa chỉ mặc định thất bại'
      );
    }
  };

  const getAddressTypeIcon = (type) => {
    switch (type) {
      case 'HOME':
        return <Home className="w-4 h-4" />;
      case 'OFFICE':
        return <Briefcase className="w-4 h-4" />;
      default:
        return <MapPinned className="w-4 h-4" />;
    }
  };

  const getAddressTypeLabel = (type) => {
    switch (type) {
      case 'HOME':
        return 'Nhà riêng';
      case 'OFFICE':
        return 'Văn phòng';
      default:
        return 'Khác';
    }
  };

  if (loading) {
    return <Loading message="Đang tải danh sách địa chỉ..." />;
  }

  if (error) {
    return (
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <ErrorMessage message={error} onRetry={fetchAddresses} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Địa chỉ của tôi</h1>
            <p className="text-gray-600 mt-1">
              Quản lý địa chỉ giao hàng của bạn
            </p>
          </div>
          <button
            onClick={handleAddAddress}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-lg text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            <Plus className="w-4 h-4 mr-2" />
            Thêm địa chỉ mới
          </button>
        </div>

        {/* Address List */}
        {addresses.length === 0 ? (
          <div className="bg-white rounded-lg shadow-sm p-12 text-center">
            <MapPin className="w-16 h-16 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Chưa có địa chỉ nào
            </h3>
            <p className="text-gray-600 mb-6">
              Thêm địa chỉ giao hàng để đặt hàng nhanh hơn
            </p>
            <button
              onClick={handleAddAddress}
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-lg text-sm font-medium text-white bg-primary-600 hover:bg-primary-700"
            >
              <Plus className="w-4 h-4 mr-2" />
              Thêm địa chỉ đầu tiên
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {addresses.map((address) => (
              <div
                key={address.id}
                className={`bg-white rounded-lg shadow-sm p-6 border-2 ${
                  address.isDefault
                    ? 'border-primary-500'
                    : 'border-transparent'
                }`}
              >
                {/* Header */}
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-700">
                      {getAddressTypeIcon(address.addressType)}
                      {getAddressTypeLabel(address.addressType)}
                    </span>
                    {address.isDefault && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium bg-primary-100 text-primary-800">
                        <Star className="w-3 h-3" />
                        Mặc định
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleEditAddress(address)}
                      className="text-gray-400 hover:text-primary-600"
                      title="Chỉnh sửa"
                    >
                      <Edit className="w-4 h-4" />
                    </button>
                    {!address.isDefault && (
                      <button
                        onClick={() => handleDeleteAddress(address.id)}
                        className="text-gray-400 hover:text-red-600"
                        title="Xóa"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Contact Info */}
                <div className="space-y-2 mb-4">
                  <div className="flex items-center gap-2">
                    <User className="w-4 h-4 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {address.receiverName}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Phone className="w-4 h-4 text-gray-400" />
                    <span className="text-gray-600">{address.phone}</span>
                  </div>
                </div>

                {/* Address */}
                <div className="flex items-start gap-2 mb-4">
                  <MapPin className="w-4 h-4 text-gray-400 mt-1 flex-shrink-0" />
                  <p className="text-gray-600 text-sm">{address.fullAddress}</p>
                </div>

                {/* Set Default Button */}
                {!address.isDefault && (
                  <button
                    onClick={() => handleSetDefault(address.id)}
                    className="w-full text-sm text-primary-600 hover:text-primary-700 font-medium border border-primary-300 rounded-lg py-2 hover:bg-primary-50 transition-colors"
                  >
                    Đặt làm mặc định
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add/Edit Address Modal */}
      <AddAddressModal
        isOpen={showAddModal}
        onClose={() => {
          setShowAddModal(false);
          setEditingAddress(null);
        }}
        onSuccess={fetchAddresses}
        address={editingAddress}
      />
    </div>
  );
};

export default AddressManagementPage;
