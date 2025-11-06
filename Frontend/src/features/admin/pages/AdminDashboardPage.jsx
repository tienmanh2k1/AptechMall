import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import {
  Users,
  ShoppingCart,
  DollarSign,
  Wallet,
  TrendingUp,
  UserCheck,
  Clock,
  Lock,
} from 'lucide-react';
import { getDashboardStats } from '../services/dashboardApi';

/**
 * Admin Dashboard Page
 * Overview of key metrics and statistics
 */
const AdminDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const response = await getDashboardStats();
      if (response.success && response.data) {
        setStats(response.data);
      } else {
        toast.error('Failed to load dashboard statistics');
      }
    } catch (error) {
      console.error('Error fetching dashboard stats:', error);
      toast.error(error.response?.data?.message || 'Failed to load dashboard statistics');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const formatNumber = (num) => {
    return new Intl.NumberFormat('vi-VN').format(num);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  const StatCard = ({ title, value, icon: Icon, color, subtitle }) => (
    <div className={`bg-white rounded-lg shadow-sm p-6 border-l-4 ${color}`}>
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-1">{title}</p>
          <p className="text-3xl font-bold text-gray-900">{value}</p>
          {subtitle && <p className="text-sm text-gray-500 mt-1">{subtitle}</p>}
        </div>
        <div className={`p-3 rounded-lg ${color.replace('border', 'bg').replace('600', '100')}`}>
          <Icon className={`w-8 h-8 ${color.replace('border', 'text')}`} />
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="mt-2 text-gray-600">
            Overview of your platform's key metrics and performance
          </p>
        </div>

        {/* Main Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <StatCard
            title="Total Users"
            value={formatNumber(stats.totalUsers || 0)}
            icon={Users}
            color="border-blue-600"
            subtitle={`${formatNumber(stats.activeUsers || 0)} active`}
          />
          <StatCard
            title="Total Orders"
            value={formatNumber(stats.totalOrders || 0)}
            icon={ShoppingCart}
            color="border-green-600"
            subtitle={`${formatNumber(stats.pendingOrders || 0)} pending`}
          />
          <StatCard
            title="Total Revenue"
            value={formatCurrency(stats.totalRevenue || 0)}
            icon={DollarSign}
            color="border-purple-600"
            subtitle="From completed orders"
          />
          <StatCard
            title="Total Wallets"
            value={formatNumber(stats.totalWallets || 0)}
            icon={Wallet}
            color="border-orange-600"
            subtitle={`${formatNumber(stats.lockedWallets || 0)} locked`}
          />
        </div>

        {/* Detailed Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* User Stats */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center mb-4">
              <Users className="w-6 h-6 text-blue-600 mr-2" />
              <h2 className="text-lg font-semibold text-gray-900">User Statistics</h2>
            </div>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                <div className="flex items-center">
                  <UserCheck className="w-5 h-5 text-blue-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Active Users</span>
                </div>
                <span className="text-lg font-bold text-blue-600">
                  {formatNumber(stats.activeUsers || 0)}
                </span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center">
                  <Users className="w-5 h-5 text-gray-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Total Registered</span>
                </div>
                <span className="text-lg font-bold text-gray-900">
                  {formatNumber(stats.totalUsers || 0)}
                </span>
              </div>
              <div className="pt-3 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Active Rate</span>
                  <span className="text-sm font-semibold text-green-600">
                    {stats.totalUsers > 0
                      ? `${((stats.activeUsers / stats.totalUsers) * 100).toFixed(1)}%`
                      : '0%'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Order Stats */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center mb-4">
              <ShoppingCart className="w-6 h-6 text-green-600 mr-2" />
              <h2 className="text-lg font-semibold text-gray-900">Order Statistics</h2>
            </div>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-yellow-50 rounded-lg">
                <div className="flex items-center">
                  <Clock className="w-5 h-5 text-yellow-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Pending Orders</span>
                </div>
                <span className="text-lg font-bold text-yellow-600">
                  {formatNumber(stats.pendingOrders || 0)}
                </span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center">
                  <ShoppingCart className="w-5 h-5 text-gray-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Total Orders</span>
                </div>
                <span className="text-lg font-bold text-gray-900">
                  {formatNumber(stats.totalOrders || 0)}
                </span>
              </div>
              <div className="pt-3 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Completion Rate</span>
                  <span className="text-sm font-semibold text-green-600">
                    {stats.totalOrders > 0
                      ? `${(((stats.totalOrders - stats.pendingOrders) / stats.totalOrders) * 100).toFixed(1)}%`
                      : '0%'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Revenue Stats */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center mb-4">
              <TrendingUp className="w-6 h-6 text-purple-600 mr-2" />
              <h2 className="text-lg font-semibold text-gray-900">Revenue Statistics</h2>
            </div>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-purple-50 rounded-lg">
                <div className="flex items-center">
                  <DollarSign className="w-5 h-5 text-purple-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Total Revenue</span>
                </div>
                <span className="text-lg font-bold text-purple-600">
                  {formatCurrency(stats.totalRevenue || 0)}
                </span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center">
                  <ShoppingCart className="w-5 h-5 text-gray-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Avg. Order Value</span>
                </div>
                <span className="text-lg font-bold text-gray-900">
                  {stats.totalOrders > 0
                    ? formatCurrency(stats.totalRevenue / stats.totalOrders)
                    : formatCurrency(0)}
                </span>
              </div>
            </div>
          </div>

          {/* Wallet Stats */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center mb-4">
              <Wallet className="w-6 h-6 text-orange-600 mr-2" />
              <h2 className="text-lg font-semibold text-gray-900">Wallet Statistics</h2>
            </div>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                <div className="flex items-center">
                  <Lock className="w-5 h-5 text-red-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Locked Wallets</span>
                </div>
                <span className="text-lg font-bold text-red-600">
                  {formatNumber(stats.lockedWallets || 0)}
                </span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center">
                  <Wallet className="w-5 h-5 text-gray-600 mr-3" />
                  <span className="text-sm font-medium text-gray-700">Total Balance</span>
                </div>
                <span className="text-lg font-bold text-gray-900">
                  {formatCurrency(stats.totalWalletBalance || 0)}
                </span>
              </div>
              <div className="pt-3 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Avg. Balance</span>
                  <span className="text-sm font-semibold text-orange-600">
                    {stats.totalWallets > 0
                      ? formatCurrency(stats.totalWalletBalance / stats.totalWallets)
                      : formatCurrency(0)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Refresh Button */}
        <div className="mt-8 text-center">
          <button
            onClick={fetchStats}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
          >
            <TrendingUp className="w-4 h-4 mr-2" />
            Refresh Statistics
          </button>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboardPage;
