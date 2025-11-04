import React, { useState, useEffect } from 'react';
import { getOrders } from '../services';
import OrderItem from '../components/OrderItem';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import { Package } from 'lucide-react';

const OrderListPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const fetchOrders = async (page = 1, status = null) => {
    try {
      setLoading(true);
      setError(null);
      // User ID is automatically extracted from JWT token by backend
      // Backend uses 0-indexed pages, so convert from 1-indexed (page - 1)
      const data = await getOrders(page - 1, 10, status || null);

      setOrders(data.orders || data.content || data);
      setTotalPages(data.totalPages || 1);
      setCurrentPage(page);
    } catch (err) {
      console.error('Error fetching orders:', err);
      setError(err.response?.data?.message || 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders(1, statusFilter);
  }, [statusFilter]);

  const handleStatusFilterChange = (status) => {
    setStatusFilter(status);
    setCurrentPage(1);
  };

  const handlePageChange = (page) => {
    fetchOrders(page, statusFilter);
  };

  // Loading state
  if (loading && orders.length === 0) {
    return <Loading message="Loading your orders..." />;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">My Orders</h1>
      </div>

      {/* Status Filter */}
      <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
        <button
          onClick={() => handleStatusFilterChange('')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            statusFilter === ''
              ? 'bg-red-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          All
        </button>
        <button
          onClick={() => handleStatusFilterChange('PENDING')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            statusFilter === 'PENDING'
              ? 'bg-red-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          Pending
        </button>
        <button
          onClick={() => handleStatusFilterChange('CONFIRMED')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            statusFilter === 'CONFIRMED'
              ? 'bg-red-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          Confirmed
        </button>
        <button
          onClick={() => handleStatusFilterChange('SHIPPING')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            statusFilter === 'SHIPPING'
              ? 'bg-red-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          Shipping
        </button>
        <button
          onClick={() => handleStatusFilterChange('DELIVERED')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            statusFilter === 'DELIVERED'
              ? 'bg-red-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          Delivered
        </button>
      </div>

      {/* Error State */}
      {error && (
        <ErrorMessage
          message={error}
          onRetry={() => fetchOrders(currentPage, statusFilter)}
        />
      )}

      {/* Orders List */}
      {!error && orders.length === 0 && !loading && (
        <div className="flex flex-col items-center justify-center py-16 px-4">
          <div className="bg-gray-100 rounded-full p-8 mb-6">
            <Package className="w-16 h-16 text-gray-400" />
          </div>
          <h2 className="text-2xl font-semibold text-gray-900 mb-2">
            No orders found
          </h2>
          <p className="text-gray-600 mb-8 text-center max-w-md">
            {statusFilter
              ? `You don't have any ${statusFilter.toLowerCase()} orders yet.`
              : "You haven't placed any orders yet. Start shopping to see your orders here!"}
          </p>
        </div>
      )}

      {!error && orders.length > 0 && (
        <>
          <div className="space-y-4 mb-8">
            {orders.map((order) => (
              <OrderItem key={order.id} order={order} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1 || loading}
                className="px-4 py-2 rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="px-4 py-2 text-gray-700">
                Page {currentPage} of {totalPages}
              </span>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages || loading}
                className="px-4 py-2 rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default OrderListPage;
