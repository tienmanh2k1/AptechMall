import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Copy, CheckCircle, RefreshCw, Building2, QrCode, Download, MessageSquare, AlertTriangle, Smartphone } from 'lucide-react';
import { jwtDecode } from 'jwt-decode';
import { getWallet } from '../services/walletApi';
import { processPendingSms } from '../services/bankTransferApi';
import { generateVietQR, BANK_BINS, downloadQR } from '../services/vietQrService';

/**
 * Bank Transfer Deposit Page with QR Code
 * Shows bank account details, QR code for easy scanning, and transfer instructions
 */
const BankTransferDepositPage = () => {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState('');
  const [checking, setChecking] = useState(false);
  const [showQR, setShowQR] = useState(true);

  // Bank account details (REPLACE with actual account)
  const bankAccount = {
    bankName: 'Mbbank',
    bankBin: BANK_BINS.MBBANK, // Vietcombank BIN code
    accountNumber: '0975299279',
    accountName: 'Nguyen Duc Luong',
    branch: 'Nguyen Duc Luong'
  };

  useEffect(() => {
    loadWallet();
  }, []);

  const loadWallet = async () => {
    try {
      setLoading(true);
      const response = await getWallet();
      console.log('Wallet data:', response.data); // Debug: Check depositCode
      setWallet(response.data);
    } catch (err) {
      console.error('Error loading wallet:', err);
    } finally {
      setLoading(false);
    }
  };

  // Copy to clipboard
  const copyToClipboard = (text, field) => {
    navigator.clipboard.writeText(text);
    setCopied(field);
    setTimeout(() => setCopied(''), 2000);
  };

  // Check for new deposits (trigger SMS processing)
  const checkForDeposit = async () => {
    try {
      setChecking(true);
      await processPendingSms();
      await loadWallet();
      alert('Checked for new deposits. If you just transferred, please wait 1-2 minutes for bank SMS.');
    } catch (err) {
      console.error('Error checking deposits:', err);
      alert('Error checking deposits. Please try again.');
    } finally {
      setChecking(false);
    }
  };

  // Get deposit code - always use USER{userId} format (safest for bank transfers)
  const getDepositCode = () => {
    if (wallet?.userId) {
      return `USER${wallet.userId}`;
    }

    console.warn('No user ID available. User may not be logged in.');
    return null;
  };

  const depositCode = getDepositCode();

  // Debug: Log deposit code on wallet load
  useEffect(() => {
    if (wallet) {
      console.log('Deposit code for bank transfer:', depositCode);
      if (!depositCode) {
        console.warn('No deposit code available. User may not be logged in.');
      }
    }
  }, [wallet, depositCode]);

  const handleDownloadQR = async () => {
    try {
      const transferContent = depositCode ? `NAP TIEN ${depositCode}` : 'NAP TIEN';
      const qrUrl = generateVietQR({
        bankBin: bankAccount.bankBin,
        accountNo: bankAccount.accountNumber,
        accountName: bankAccount.accountName,
        description: transferContent
      });

      await downloadQR(qrUrl, `bank-transfer-qr-${depositCode}.png`);
    } catch (err) {
      console.error('Error downloading QR:', err);
      alert('Failed to download QR code');
    }
  };

  const transferContent = depositCode ? `NAP TIEN ${depositCode}` : 'NAP TIEN ...';

  // Generate QR code URL
  const qrCodeUrl = depositCode ? generateVietQR({
    bankBin: bankAccount.bankBin,
    accountNo: bankAccount.accountNumber,
    accountName: bankAccount.accountName,
    description: transferContent
  }) : null;

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-2xl">
        {/* Header */}
        <button
          onClick={() => navigate('/wallet')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-800 mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          Back to Wallet
        </button>

        <h1 className="text-3xl font-bold text-gray-800 mb-6">Bank Transfer Deposit</h1>

        {/* QR Code Section */}
        {showQR && qrCodeUrl && (
          <div className="bg-gradient-to-br from-blue-500 to-blue-700 rounded-2xl shadow-xl p-8 mb-6 text-white">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-bold flex items-center gap-2">
                <Smartphone className="w-7 h-7" />
                Scan QR to Pay
              </h2>
              <button
                onClick={handleDownloadQR}
                className="flex items-center gap-2 bg-white text-blue-600 px-4 py-2 rounded-lg hover:bg-blue-50 transition font-semibold"
              >
                <Download className="w-4 h-4" />
                Download
              </button>
            </div>

            <div className="bg-white rounded-xl p-6 mb-4">
              <img
                src={qrCodeUrl}
                alt="VietQR Payment QR Code"
                className="w-full max-w-sm mx-auto"
                onError={(e) => {
                  console.error('QR code failed to load');
                  e.target.style.display = 'none';
                }}
              />
            </div>

            <div className="space-y-2 text-sm">
              <p className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                Open your banking app (Vietcombank, MB Bank, etc.)
              </p>
              <p className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                Select "Scan QR" or "Transfer via QR"
              </p>
              <p className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                Scan this QR code - all info will auto-fill
              </p>
              <p className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                Confirm and complete transfer
              </p>
            </div>

            <button
              onClick={() => setShowQR(false)}
              className="mt-4 text-sm text-blue-100 hover:text-white underline"
            >
              Or enter manually instead
            </button>
          </div>
        )}

        {/* Warning Box */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <div className="flex gap-3">
            <AlertTriangle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-yellow-800 mb-1">Important</h4>
              <ul className="text-sm text-yellow-700 space-y-1 list-disc ml-4">
                <li>Only transfer from YOUR bank account</li>
                <li>MUST include transfer content exactly as shown</li>
                <li>Balance updates automatically within 1-2 minutes</li>
                <li>Keep SMS notification from bank for verification</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Manual Transfer Details */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
              <Building2 className="w-6 h-6 text-blue-500" />
              Bank Account Details
            </h2>
            {!showQR && (
              <button
                onClick={() => setShowQR(true)}
                className="flex items-center gap-2 text-blue-600 hover:text-blue-700 text-sm font-medium"
              >
                <QrCode className="w-4 h-4" />
                Show QR Code
              </button>
            )}
          </div>

          <div className="space-y-4">
            {/* Bank Name */}
            <div className="border-b pb-3">
              <p className="text-sm text-gray-600 mb-1">Bank Name</p>
              <div className="flex items-center justify-between">
                <p className="text-lg font-semibold text-gray-800">{bankAccount.bankName}</p>
                <button
                  onClick={() => copyToClipboard(bankAccount.bankName, 'bank')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition"
                >
                  {copied === 'bank' ? (
                    <CheckCircle className="w-5 h-5 text-green-500" />
                  ) : (
                    <Copy className="w-5 h-5 text-gray-400" />
                  )}
                </button>
              </div>
            </div>

            {/* Account Number */}
            <div className="border-b pb-3">
              <p className="text-sm text-gray-600 mb-1">Account Number</p>
              <div className="flex items-center justify-between">
                <p className="text-2xl font-bold text-blue-600 tracking-wider">
                  {bankAccount.accountNumber}
                </p>
                <button
                  onClick={() => copyToClipboard(bankAccount.accountNumber, 'account')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition"
                >
                  {copied === 'account' ? (
                    <CheckCircle className="w-5 h-5 text-green-500" />
                  ) : (
                    <Copy className="w-5 h-5 text-gray-400" />
                  )}
                </button>
              </div>
            </div>

            {/* Account Name */}
            <div className="border-b pb-3">
              <p className="text-sm text-gray-600 mb-1">Account Name</p>
              <div className="flex items-center justify-between">
                <p className="text-lg font-semibold text-gray-800">{bankAccount.accountName}</p>
                <button
                  onClick={() => copyToClipboard(bankAccount.accountName, 'name')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition"
                >
                  {copied === 'name' ? (
                    <CheckCircle className="w-5 h-5 text-green-500" />
                  ) : (
                    <Copy className="w-5 h-5 text-gray-400" />
                  )}
                </button>
              </div>
            </div>

            {/* Branch */}
            <div className="border-b pb-3">
              <p className="text-sm text-gray-600 mb-1">Branch</p>
              <p className="text-lg text-gray-800">{bankAccount.branch}</p>
            </div>

            {/* Transfer Content */}
            <div className="bg-red-50 border-2 border-red-300 rounded-lg p-4">
              <p className="text-sm text-red-600 font-semibold mb-2 flex items-center gap-2">
                <MessageSquare className="w-4 h-4" />
                Transfer Content (REQUIRED)
              </p>
              <div className="flex items-center justify-between mb-2">
                <p className="text-xl font-bold text-red-700">{transferContent}</p>
                <button
                  onClick={() => copyToClipboard(transferContent, 'content')}
                  className="p-2 bg-red-100 hover:bg-red-200 rounded-lg transition"
                >
                  {copied === 'content' ? (
                    <CheckCircle className="w-5 h-5 text-green-500" />
                  ) : (
                    <Copy className="w-5 h-5 text-red-600" />
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Check Button */}
        <button
          onClick={checkForDeposit}
          disabled={checking}
          className="w-full bg-blue-500 text-white py-4 px-6 rounded-lg font-semibold text-lg hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {checking ? (
            <>
              <RefreshCw className="w-5 h-5 animate-spin" />
              Checking...
            </>
          ) : (
            <>
              <RefreshCw className="w-5 h-5" />
              Check for Deposit
            </>
          )}
        </button>

        <p className="text-sm text-gray-500 text-center mt-4">
          Your balance will update automatically when we receive the bank SMS notification
        </p>
      </div>
    </div>
  );
};

export default BankTransferDepositPage;
