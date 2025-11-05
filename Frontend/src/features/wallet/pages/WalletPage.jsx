import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Wallet, RefreshCw, History, AlertCircle, ArrowDownCircle } from 'lucide-react';
import { getWallet } from '../services/walletApi';

/**
 * Wallet Page
 * Main wallet dashboard showing balance and deposit options
 */
const WalletPage = () => {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load wallet data
  const loadWallet = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await getWallet();
      setWallet(response.data);
    } catch (err) {
      console.error('Error loading wallet:', err);
      setError(err.response?.data?.message || 'Failed to load wallet');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWallet();
  }, []);

  // Format currency
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount || 0);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="w-8 h-8 animate-spin text-blue-500 mx-auto mb-2" />
          <p className="text-gray-600">Loading wallet...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-md p-6 max-w-md">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-gray-800 text-center mb-2">Error</h2>
          <p className="text-gray-600 text-center mb-4">{error}</p>
          <button
            onClick={loadWallet}
            className="w-full bg-blue-500 text-white py-2 px-4 rounded-lg hover:bg-blue-600 transition"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            <Wallet className="w-8 h-8" />
            My Wallet
          </h1>
          <button
            onClick={loadWallet}
            className="flex items-center gap-2 bg-white px-4 py-2 rounded-lg shadow hover:shadow-md transition"
          >
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>

        {/* Wallet Balance Card */}
        <div className="bg-gradient-to-br from-blue-500 to-blue-700 rounded-2xl shadow-xl p-8 mb-6 text-white">
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-blue-100 text-sm mb-1">Available Balance</p>
              <h2 className="text-4xl font-bold">
                {formatCurrency(wallet?.balance)}
              </h2>
            </div>
            <Wallet className="w-16 h-16 text-blue-200 opacity-50" />
          </div>

          {wallet?.isLocked && (
            <div className="bg-red-500 bg-opacity-30 border border-red-300 rounded-lg p-3 mt-4">
              <p className="text-sm font-medium flex items-center gap-2">
                <AlertCircle className="w-4 h-4" />
                Wallet is locked. Contact support for assistance.
              </p>
            </div>
          )}

          <div className="flex gap-2 text-xs text-blue-100 mt-4">
            <span>Wallet ID: {wallet?.walletId}</span>
            <span>•</span>
            <span>Created: {new Date(wallet?.createdAt).toLocaleDateString('vi-VN')}</span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          {/* Bank Transfer Deposit */}
          <button
            onClick={() => navigate('/wallet/deposit/bank-transfer')}
            disabled={wallet?.isLocked}
            className="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition disabled:opacity-50 disabled:cursor-not-allowed text-left"
          >
            <ArrowDownCircle className="w-10 h-10 text-green-500 mb-3" />
            <h3 className="text-lg font-bold text-gray-800 mb-1">Bank Transfer</h3>
            <p className="text-sm text-gray-600">
              Deposit via bank transfer with SMS verification
            </p>
          </button>

          {/* Transaction History */}
          <button
            onClick={() => navigate('/wallet/transactions')}
            className="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition text-left"
          >
            <History className="w-10 h-10 text-blue-500 mb-3" />
            <h3 className="text-lg font-bold text-gray-800 mb-1">Transaction History</h3>
            <p className="text-sm text-gray-600">
              View all your wallet transactions
            </p>
          </button>
        </div>

        {/* Info Box */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="font-semibold text-blue-800 mb-2 flex items-center gap-2">
            <AlertCircle className="w-5 h-5" />
            How to deposit
          </h4>
          <ul className="text-sm text-blue-700 space-y-1 ml-6 list-disc">
            <li>Click "Bank Transfer" to see bank account details</li>
            <li>Transfer money from your bank to our account</li>
            <li>Include your email in transfer note or scan QR code</li>
            <li>SMS from bank will be processed automatically</li>
            <li>Balance updates within 1-2 minutes</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default WalletPage;
