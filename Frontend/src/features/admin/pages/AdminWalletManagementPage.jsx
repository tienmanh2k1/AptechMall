import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { Search, Lock, Unlock, DollarSign, User, Mail } from 'lucide-react';
import { getAllWallets, lockWallet, unlockWallet } from '../services/walletAdminApi';

/**
 * Admin Wallet Management Page
 * Allows administrators to view and manage user wallets
 */
const AdminWalletManagementPage = () => {
  const [wallets, setWallets] = useState([]);
  const [filteredWallets, setFilteredWallets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [lockedFilter, setLockedFilter] = useState('all');

  useEffect(() => {
    fetchWallets();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [wallets, searchTerm, lockedFilter]);

  const fetchWallets = async () => {
    setLoading(true);
    try {
      const response = await getAllWallets();
      if (response.success && response.data) {
        setWallets(response.data);
      } else {
        toast.error('Failed to load wallets');
      }
    } catch (error) {
      console.error('Error fetching wallets:', error);
      toast.error(error.response?.data?.message || 'Failed to load wallets');
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...wallets];

    // Apply search filter
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(wallet =>
        wallet.username?.toLowerCase().includes(term) ||
        wallet.email?.toLowerCase().includes(term) ||
        wallet.fullName?.toLowerCase().includes(term) ||
        wallet.userId?.toString().includes(term)
      );
    }

    // Apply locked status filter
    if (lockedFilter !== 'all') {
      const isLocked = lockedFilter === 'locked';
      filtered = filtered.filter(wallet => wallet.isLocked === isLocked);
    }

    setFilteredWallets(filtered);
  };

  const handleLockWallet = async (userId, username) => {
    if (!window.confirm(`Are you sure you want to lock ${username}'s wallet?`)) {
      return;
    }

    try {
      const response = await lockWallet(userId);
      if (response.success) {
        toast.success(`Locked wallet for ${username}`);
        fetchWallets(); // Refresh the list
      } else {
        toast.error(response.message || 'Failed to lock wallet');
      }
    } catch (error) {
      console.error('Error locking wallet:', error);
      toast.error(error.response?.data?.message || 'Failed to lock wallet');
    }
  };

  const handleUnlockWallet = async (userId, username) => {
    if (!window.confirm(`Are you sure you want to unlock ${username}'s wallet?`)) {
      return;
    }

    try {
      const response = await unlockWallet(userId);
      if (response.success) {
        toast.success(`Unlocked wallet for ${username}`);
        fetchWallets(); // Refresh the list
      } else {
        toast.error(response.message || 'Failed to unlock wallet');
      }
    } catch (error) {
      console.error('Error unlocking wallet:', error);
      toast.error(error.response?.data?.message || 'Failed to unlock wallet');
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('vi-VN');
  };

  const getLockedBadge = (isLocked) => {
    if (isLocked) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
          <Lock className="w-3 h-3 mr-1" />
          Locked
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
        <Unlock className="w-3 h-3 mr-1" />
        Active
      </span>
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading wallets...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Wallet Management</h1>
          <p className="mt-2 text-gray-600">
            Manage user wallets and monitor balances
          </p>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Search */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Search
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  placeholder="Search by username, email, or user ID..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Locked Status Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Status
              </label>
              <select
                value={lockedFilter}
                onChange={(e) => setLockedFilter(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
              >
                <option value="all">All Wallets</option>
                <option value="active">Active Only</option>
                <option value="locked">Locked Only</option>
              </select>
            </div>
          </div>

          {/* Stats */}
          <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-blue-50 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-blue-600 font-medium">Total Wallets</p>
                  <p className="text-2xl font-bold text-blue-900">{wallets.length}</p>
                </div>
                <DollarSign className="w-8 h-8 text-blue-600" />
              </div>
            </div>
            <div className="bg-green-50 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-green-600 font-medium">Active Wallets</p>
                  <p className="text-2xl font-bold text-green-900">
                    {wallets.filter(w => !w.isLocked).length}
                  </p>
                </div>
                <Unlock className="w-8 h-8 text-green-600" />
              </div>
            </div>
            <div className="bg-red-50 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-red-600 font-medium">Locked Wallets</p>
                  <p className="text-2xl font-bold text-red-900">
                    {wallets.filter(w => w.isLocked).length}
                  </p>
                </div>
                <Lock className="w-8 h-8 text-red-600" />
              </div>
            </div>
          </div>
        </div>

        {/* Wallets Table */}
        <div className="bg-white rounded-lg shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Contact
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Balance
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Created At
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredWallets.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-gray-500">
                      No wallets found
                    </td>
                  </tr>
                ) : (
                  filteredWallets.map((wallet) => (
                    <tr key={wallet.walletId} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="flex-shrink-0 h-10 w-10 bg-gray-200 rounded-full flex items-center justify-center">
                            <User className="w-5 h-5 text-gray-500" />
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-gray-900">
                              {wallet.fullName || 'N/A'}
                            </div>
                            <div className="text-sm text-gray-500">
                              @{wallet.username || 'N/A'}
                            </div>
                            <div className="text-xs text-gray-400">
                              ID: {wallet.userId}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center text-sm text-gray-900">
                          <Mail className="w-4 h-4 mr-2 text-gray-400" />
                          {wallet.email || 'N/A'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-semibold text-gray-900">
                          {formatCurrency(wallet.balance || 0)}
                        </div>
                        <div className="text-xs text-gray-500">
                          Code: {wallet.depositCode}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getLockedBadge(wallet.isLocked)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(wallet.createdAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        {wallet.isLocked ? (
                          <button
                            onClick={() => handleUnlockWallet(wallet.userId, wallet.username)}
                            className="inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                          >
                            <Unlock className="w-4 h-4 mr-1" />
                            Unlock
                          </button>
                        ) : (
                          <button
                            onClick={() => handleLockWallet(wallet.userId, wallet.username)}
                            className="inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                          >
                            <Lock className="w-4 h-4 mr-1" />
                            Lock
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Results Count */}
        {filteredWallets.length > 0 && (
          <div className="mt-4 text-sm text-gray-600 text-center">
            Showing {filteredWallets.length} of {wallets.length} wallets
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminWalletManagementPage;
