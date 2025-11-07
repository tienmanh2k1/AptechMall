import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { getAllAddresses } from '../../user/services/addressApi';
import AddAddressModal from '../../user/components/AddAddressModal';
import {
  X,
  MapPin,
  Star,
  Plus,
  Home,
  Briefcase,
  MapPinned,
  User,
  Phone,
  Check,
} from 'lucide-react';

/**
 * Address Selector Modal for Checkout
 * Allows user to select from saved addresses or add new one
 */
const AddressSelectorModal = ({ isOpen, onClose, onSelectAddress, selectedAddressId }) => {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  const fetchAddresses = async () => {
    try {
      setLoading(true);
      const data = await getAllAddresses();
      setAddresses(data);
    } catch (error) {
      console.error('Error fetching addresses:', error);
      toast.error('Không thể tải danh sách địa chỉ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchAddresses();
    }
  }, [isOpen]);

  const handleSelectAddress = (address) => {
    onSelectAddress(address);
    onClose();
  };

  const handleAddNewAddress = () => {
    setShowAddModal(true);
  };

  const handleAddressAdded = () => {
    fetchAddresses();
    setShowAddModal(false);
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

  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 z-50 overflow-y-auto">
        {/* Overlay */}
        <div
          className="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
          onClick={onClose}
        />

        {/* Modal */}
        <div className="flex min-h-full items-center justify-center p-4">
          <div className="relative bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[80vh] overflow-y-auto">
            {/* Header */}
            <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
              <div className="flex items-center">
                <MapPin className="w-5 h-5 text-primary-600 mr-2" />
                <h3 className="text-lg font-semibold text-gray-900">
                  Chọn địa chỉ giao hàng
                </h3>
              </div>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-500 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="p-6">
              {loading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
                  <p className="text-gray-600 mt-2">Đang tải...</p>
                </div>
              ) : addresses.length === 0 ? (
                <div className="text-center py-8">
                  <MapPin className="w-16 h-16 mx-auto text-gray-400 mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">
                    Chưa có địa chỉ nào
                  </h3>
                  <p className="text-gray-600 mb-6">
                    Thêm địa chỉ giao hàng để tiếp tục
                  </p>
                  <button
                    onClick={handleAddNewAddress}
                    className="inline-flex items-center px-4 py-2 border border-transparent rounded-lg text-sm font-medium text-white bg-primary-600 hover:bg-primary-700"
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    Thêm địa chỉ mới
                  </button>
                </div>
              ) : (
                <>
                  {/* Add New Address Button */}
                  <button
                    onClick={handleAddNewAddress}
                    className="w-full mb-4 py-3 border-2 border-dashed border-gray-300 rounded-lg text-sm font-medium text-gray-600 hover:border-primary-500 hover:text-primary-600 transition-colors flex items-center justify-center gap-2"
                  >
                    <Plus className="w-4 h-4" />
                    Thêm địa chỉ mới
                  </button>

                  {/* Address List */}
                  <div className="space-y-3">
                    {addresses.map((address) => (
                      <button
                        key={address.id}
                        onClick={() => handleSelectAddress(address)}
                        className={`w-full text-left p-4 border-2 rounded-lg transition-all hover:border-primary-500 ${
                          selectedAddressId === address.id
                            ? 'border-primary-500 bg-primary-50'
                            : 'border-gray-200'
                        }`}
                      >
                        <div className="flex items-start justify-between mb-2">
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
                          {selectedAddressId === address.id && (
                            <div className="bg-primary-600 rounded-full p-1">
                              <Check className="w-4 h-4 text-white" />
                            </div>
                          )}
                        </div>

                        <div className="space-y-2">
                          <div className="flex items-center gap-2">
                            <User className="w-4 h-4 text-gray-400 flex-shrink-0" />
                            <span className="font-medium text-gray-900">
                              {address.receiverName}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Phone className="w-4 h-4 text-gray-400 flex-shrink-0" />
                            <span className="text-gray-600">{address.phone}</span>
                          </div>
                          <div className="flex items-start gap-2">
                            <MapPin className="w-4 h-4 text-gray-400 mt-1 flex-shrink-0" />
                            <p className="text-gray-600 text-sm">
                              {address.fullAddress}
                            </p>
                          </div>
                        </div>
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Add Address Modal */}
      <AddAddressModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSuccess={handleAddressAdded}
      />
    </>
  );
};

export default AddressSelectorModal;
