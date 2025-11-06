import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, ArrowUpCircle, ArrowDownCircle, Filter, ChevronLeft, ChevronRight } from 'lucide-react';
import { getTransactionHistory } from '../services/walletApi';

/**
 * Transaction History Page
 * Shows paginated list of wallet transactions with filters
 */
const TransactionHistoryPage = () => {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filter, setFilter] = useState('');

  const pageSize = 10;

  // Load transactions
  const loadTransactions = async () => {
    try {
      setLoading(true);
      setError(null);
      const filters = { page, size: pageSize };
      if (filter) {
        filters.transactionType = filter;
      }

      const response = await getTransactionHistory(filters);
      setTransactions(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch (err) {
      console.error('Error loading transactions:', err);
      setError(err.response?.data?.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTransactions();
  }, [page, filter]);

  // Format currency
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount || 0);
  };

  // Format date
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN');
  };

  // Get transaction type badge color
  const getTypeColor = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return 'bg-green-100 text-green-800';
      case 'WITHDRAWAL':
        return 'bg-red-100 text-red-800';
      case 'ORDER_PAYMENT':
        return 'bg-blue-100 text-blue-800';
      case 'ORDER_REFUND':
        return 'bg-purple-100 text-purple-800';
      case 'ADMIN_ADJUSTMENT':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  // Get transaction type label
  const getTypeLabel = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return 'Deposit';
      case 'WITHDRAWAL':
        return 'Withdrawal';
      case 'ORDER_PAYMENT':
        return 'Order Payment';
      case 'ORDER_REFUND':
        return 'Refund';
      case 'ADMIN_ADJUSTMENT':
        return 'Admin Adjustment';
      default:
        return type;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-6xl">
        {/* Header */}
        <button
          onClick={() => navigate('/wallet')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-800 mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          Back to Wallet
        </button>

        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl font-bold text-gray-800">Transaction History</h1>
          <button
            onClick={loadTransactions}
            className="flex items-center gap-2 bg-white px-4 py-2 rounded-lg shadow hover:shadow-md transition"
          >
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>

        {/* Filter */}
        <div className="bg-white rounded-lg shadow-md p-4 mb-6">
          <div className="flex items-center gap-4">
            <Filter className="w-5 h-5 text-gray-500" />
            <select
              value={filter}
              onChange={(e) => {
                setFilter(e.target.value);
                setPage(0);
              }}
              className="flex-1 border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Transactions</option>
              <option value="DEPOSIT">Deposits</option>
              <option value="WITHDRAWAL">Withdrawals</option>
              <option value="ORDER_PAYMENT">Order Payments</option>
              <option value="ORDER_REFUND">Refunds</option>
              <option value="ADMIN_ADJUSTMENT">Admin Adjustments</option>
            </select>
            <span className="text-sm text-gray-600">
              Total: {totalElements} transactions
            </span>
          </div>
        </div>

        {/* Transaction List */}
        {loading ? (
          <div className="text-center py-12">
            <RefreshCw className="w-8 h-8 animate-spin text-blue-500 mx-auto mb-2" />
            <p className="text-gray-600">Loading transactions...</p>
          </div>
        ) : error ? (
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <p className="text-red-600 mb-4">{error}</p>
            <button
              onClick={loadTransactions}
              className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition"
            >
              Retry
            </button>
          </div>
        ) : transactions.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-12 text-center">
            <p className="text-gray-500 text-lg">No transactions found</p>
          </div>
        ) : (
          <div className="space-y-4">
            {transactions.map((tx) => (
              <div
                key={tx.transactionId}
                className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition"
              >
                <div className="flex items-start justify-between">
                  {/* Left side - Type and Description */}
                  <div className="flex gap-4">
                    <div className="flex-shrink-0">
                      {tx.credit ? (
                        <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
                          <ArrowDownCircle className="w-6 h-6 text-green-600" />
                        </div>
                      ) : (
                        <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                          <ArrowUpCircle className="w-6 h-6 text-red-600" />
                        </div>
                      )}
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold text-gray-800">{tx.description}</h3>
                        <span className={`px-2 py-1 text-xs font-medium rounded ${getTypeColor(tx.transactionType)}`}>
                          {getTypeLabel(tx.transactionType)}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 mb-1">
                        {formatDate(tx.createdAt)}
                      </p>
                      {tx.referenceNumber && (
                        <p className="text-xs text-gray-500">
                          Ref: {tx.referenceNumber}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Right side - Amount */}
                  <div className="text-right">
                    <p className={`text-2xl font-bold ${tx.credit ? 'text-green-600' : 'text-red-600'}`}>
                      {tx.credit ? '+' : '-'}{formatCurrency(tx.amount)}
                    </p>
                    <p className="text-sm text-gray-500 mt-1">
                      Balance: {formatCurrency(tx.balanceAfter)}
                    </p>
                  </div>
                </div>

                {/* Additional Info */}
                {(tx.orderId || tx.note) && (
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    {tx.orderId && (
                      <p className="text-sm text-gray-600">
                        Order ID: #{tx.orderId}
                      </p>
                    )}
                    {tx.note && (
                      <p className="text-sm text-gray-600">
                        Note: {tx.note}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-4 mt-8">
            <button
              onClick={() => setPage(page - 1)}
              disabled={page === 0}
              className="flex items-center gap-2 px-4 py-2 bg-white rounded-lg shadow hover:shadow-md transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </button>

            <span className="text-gray-600">
              Page {page + 1} of {totalPages}
            </span>

            <button
              onClick={() => setPage(page + 1)}
              disabled={page >= totalPages - 1}
              className="flex items-center gap-2 px-4 py-2 bg-white rounded-lg shadow hover:shadow-md transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default TransactionHistoryPage;
