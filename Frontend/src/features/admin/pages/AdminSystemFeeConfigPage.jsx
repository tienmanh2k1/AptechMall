import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import {
  Plus,
  Edit2,
  Trash2,
  X,
  Save,
  Settings,
  DollarSign,
  Percent,
  Check,
  Power
} from 'lucide-react';
import {
  getAllFeeConfigs,
  createFeeConfig,
  updateFeeConfig,
  deleteFeeConfig,
  activateFeeConfig
} from '../services/feeConfigApi';

const AdminSystemFeeConfigPage = () => {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);
  const [formData, setFormData] = useState({
    serviceFeePercent: 5.00,
    domesticShippingRate: 30000,
    internationalShippingRate: 150000,
    vietnamDomesticShippingRate: 25000,
    depositPercent: 70.00,
    isActive: false,
  });
  const [errors, setErrors] = useState({});

  // Fetch configs on mount
  useEffect(() => {
    fetchConfigs();
  }, []);

  const fetchConfigs = async () => {
    try {
      setLoading(true);
      const data = await getAllFeeConfigs();
      setConfigs(data);
    } catch (error) {
      console.error('Error fetching fee configs:', error);
      toast.error('Failed to load fee configurations. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (config = null) => {
    if (config) {
      setEditingConfig(config);
      setFormData({
        serviceFeePercent: Number(config.serviceFeePercent),
        domesticShippingRate: Number(config.domesticShippingRate),
        internationalShippingRate: Number(config.internationalShippingRate),
        vietnamDomesticShippingRate: Number(config.vietnamDomesticShippingRate),
        depositPercent: Number(config.depositPercent),
        isActive: config.isActive,
      });
    } else {
      setEditingConfig(null);
      setFormData({
        serviceFeePercent: 5.00,
        domesticShippingRate: 30000,
        internationalShippingRate: 150000,
        vietnamDomesticShippingRate: 25000,
        depositPercent: 70.00,
        isActive: false,
      });
    }
    setErrors({});
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingConfig(null);
    setFormData({
      serviceFeePercent: 5.00,
      domesticShippingRate: 30000,
      internationalShippingRate: 150000,
      vietnamDomesticShippingRate: 25000,
      depositPercent: 70.00,
      isActive: false,
    });
    setErrors({});
  };

  const validateForm = () => {
    const newErrors = {};

    if (formData.serviceFeePercent < 0 || formData.serviceFeePercent > 100) {
      newErrors.serviceFeePercent = 'Service fee must be between 0 and 100%';
    }
    if (formData.depositPercent < 0 || formData.depositPercent > 100) {
      newErrors.depositPercent = 'Deposit percent must be between 0 and 100%';
    }
    if (formData.domesticShippingRate < 0) {
      newErrors.domesticShippingRate = 'Shipping rate cannot be negative';
    }
    if (formData.internationalShippingRate < 0) {
      newErrors.internationalShippingRate = 'Shipping rate cannot be negative';
    }
    if (formData.vietnamDomesticShippingRate < 0) {
      newErrors.vietnamDomesticShippingRate = 'Shipping rate cannot be negative';
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
      if (editingConfig) {
        await updateFeeConfig(editingConfig.id, formData);
        toast.success('Fee configuration updated successfully!');
      } else {
        await createFeeConfig(formData);
        toast.success('Fee configuration created successfully!');
      }
      handleCloseModal();
      fetchConfigs();
    } catch (error) {
      console.error('Error saving fee config:', error);
      const errorMessage = error.response?.data?.message ||
                          error.response?.data?.error ||
                          'Failed to save fee configuration. Please try again.';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this fee configuration?')) {
      return;
    }

    try {
      setLoading(true);
      await deleteFeeConfig(id);
      toast.success('Fee configuration deleted successfully!');
      fetchConfigs();
    } catch (error) {
      console.error('Error deleting fee config:', error);
      const errorMessage = error.response?.data?.message ||
                          error.response?.data?.error ||
                          'Failed to delete fee configuration. Please try again.';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleActivate = async (id) => {
    try {
      setLoading(true);
      await activateFeeConfig(id);
      toast.success('Fee configuration activated successfully!');
      fetchConfigs();
    } catch (error) {
      console.error('Error activating fee config:', error);
      const errorMessage = error.response?.data?.message ||
                          error.response?.data?.error ||
                          'Failed to activate fee configuration. Please try again.';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (type === 'number' ? parseFloat(value) : value),
    }));
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 mb-2">System Fee Configuration</h1>
              <p className="text-gray-600">Manage service fees, shipping rates, and deposit requirements</p>
            </div>
            <button
              onClick={() => handleOpenModal()}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Plus className="w-5 h-5" />
              Add Configuration
            </button>
          </div>
        </div>

        {/* Configs Table */}
        {loading && configs.length === 0 ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading configurations...</p>
          </div>
        ) : configs.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <Settings className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-600 text-lg">No fee configurations found. Create your first one!</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Service Fee
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Shipping Rates
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Deposit %
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Updated
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {configs.map((config) => (
                    <tr key={config.id} className={config.isActive ? 'bg-blue-50' : 'hover:bg-gray-50'}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <span
                            className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                              config.isActive
                                ? 'bg-green-100 text-green-800'
                                : 'bg-gray-100 text-gray-800'
                            }`}
                          >
                            {config.isActive ? (
                              <><Check className="w-3 h-3 mr-1" /> Active</>
                            ) : (
                              'Inactive'
                            )}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-1 text-sm text-gray-900">
                          <Percent className="w-4 h-4 text-gray-400" />
                          <span className="font-medium">{config.serviceFeePercent}%</span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm text-gray-900 space-y-1">
                          <div>Domestic: {formatCurrency(config.domesticShippingRate)}/kg</div>
                          <div>International: {formatCurrency(config.internationalShippingRate)}/kg</div>
                          <div>VN Domestic: {formatCurrency(config.vietnamDomesticShippingRate)}/kg</div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-1 text-sm text-gray-900">
                          <DollarSign className="w-4 h-4 text-gray-400" />
                          <span className="font-medium">{config.depositPercent}%</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {config.updatedAt
                          ? new Date(config.updatedAt).toLocaleDateString()
                          : '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center justify-end gap-2">
                          {!config.isActive && (
                            <button
                              onClick={() => handleActivate(config.id)}
                              className="text-green-600 hover:text-green-900 p-1 rounded hover:bg-green-50"
                              title="Activate"
                            >
                              <Power className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleOpenModal(config)}
                            className="text-blue-600 hover:text-blue-900 p-1 rounded hover:bg-blue-50"
                            title="Edit"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDelete(config.id)}
                            className="text-red-600 hover:text-red-900 p-1 rounded hover:bg-red-50"
                            title="Delete"
                            disabled={config.isActive && configs.length === 1}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Modal */}
        {showModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
              <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
                <h2 className="text-xl font-bold text-gray-900">
                  {editingConfig ? 'Edit Fee Configuration' : 'Create New Fee Configuration'}
                </h2>
                <button
                  onClick={handleCloseModal}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="p-6 space-y-4">
                {/* Service Fee Percent */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    <Percent className="w-4 h-4 inline mr-1" />
                    Service Fee Percent (%)
                  </label>
                  <input
                    type="number"
                    name="serviceFeePercent"
                    value={formData.serviceFeePercent}
                    onChange={handleChange}
                    step="0.01"
                    min="0"
                    max="100"
                    className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.serviceFeePercent ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="5.00"
                  />
                  {errors.serviceFeePercent && (
                    <p className="mt-1 text-sm text-red-600">{errors.serviceFeePercent}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">Percentage added to product cost</p>
                </div>

                {/* Domestic Shipping Rate */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Domestic Shipping Rate (VND/kg)
                  </label>
                  <input
                    type="number"
                    name="domesticShippingRate"
                    value={formData.domesticShippingRate}
                    onChange={handleChange}
                    step="1000"
                    min="0"
                    className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.domesticShippingRate ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="30000"
                  />
                  {errors.domesticShippingRate && (
                    <p className="mt-1 text-sm text-red-600">{errors.domesticShippingRate}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">China to warehouse domestic shipping</p>
                </div>

                {/* International Shipping Rate */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    International Shipping Rate (VND/kg)
                  </label>
                  <input
                    type="number"
                    name="internationalShippingRate"
                    value={formData.internationalShippingRate}
                    onChange={handleChange}
                    step="1000"
                    min="0"
                    className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.internationalShippingRate ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="150000"
                  />
                  {errors.internationalShippingRate && (
                    <p className="mt-1 text-sm text-red-600">{errors.internationalShippingRate}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">China to Vietnam international shipping</p>
                </div>

                {/* Vietnam Domestic Shipping Rate */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Vietnam Domestic Shipping Rate (VND/kg)
                  </label>
                  <input
                    type="number"
                    name="vietnamDomesticShippingRate"
                    value={formData.vietnamDomesticShippingRate}
                    onChange={handleChange}
                    step="1000"
                    min="0"
                    className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.vietnamDomesticShippingRate ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="25000"
                  />
                  {errors.vietnamDomesticShippingRate && (
                    <p className="mt-1 text-sm text-red-600">{errors.vietnamDomesticShippingRate}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">Vietnam warehouse to customer (COD)</p>
                </div>

                {/* Deposit Percent */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    <DollarSign className="w-4 h-4 inline mr-1" />
                    Deposit Percent (%)
                  </label>
                  <input
                    type="number"
                    name="depositPercent"
                    value={formData.depositPercent}
                    onChange={handleChange}
                    step="0.01"
                    min="0"
                    max="100"
                    className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.depositPercent ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="70.00"
                  />
                  {errors.depositPercent && (
                    <p className="mt-1 text-sm text-red-600">{errors.depositPercent}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">Customer pays this percentage upfront</p>
                </div>

                {/* Active Status */}
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    name="isActive"
                    checked={formData.isActive}
                    onChange={handleChange}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label className="ml-2 block text-sm text-gray-700">
                    Set as Active (will deactivate other configurations)
                  </label>
                </div>

                {/* Form Actions */}
                <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-200">
                  <button
                    type="button"
                    onClick={handleCloseModal}
                    className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {loading ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                        <span>Saving...</span>
                      </>
                    ) : (
                      <>
                        <Save className="w-4 h-4" />
                        <span>{editingConfig ? 'Update' : 'Create'}</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminSystemFeeConfigPage;
