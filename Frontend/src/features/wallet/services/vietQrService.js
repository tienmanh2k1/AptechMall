/**
 * VietQR Service
 * Generate QR codes for bank transfers using VietQR standard
 * Compatible with all Vietnamese banking apps
 */

/**
 * Bank BIN codes (Mã định danh ngân hàng)
 */
export const BANK_BINS = {
  VIETCOMBANK: '970436',
  TECHCOMBANK: '970407',
  BIDV: '970418',
  AGRIBANK: '970405',
  MBBANK: '970422',
  VIETINBANK: '970415',
  SACOMBANK: '970403',
  ACB: '970416',
  VPBANK: '970432',
  TPBANK: '970423',
  HDBANK: '970437',
  SHB: '970443',
  OCB: '970448',
  MSB: '970426',
  SEABANK: '970440'
};

/**
 * QR code templates
 */
export const QR_TEMPLATES = {
  COMPACT: 'compact',      // Compact with logo
  COMPACT2: 'compact2',    // Compact without logo
  QR_ONLY: 'qr_only',     // QR only, no decoration
  PRINT: 'print'          // Print version with full info
};

/**
 * Generate VietQR image URL
 *
 * @param {Object} params - QR parameters
 * @param {string} params.bankBin - Bank BIN code (e.g., '970436' for Vietcombank)
 * @param {string} params.accountNo - Bank account number
 * @param {string} params.accountName - Account holder name
 * @param {number} params.amount - Transfer amount (optional, in VND)
 * @param {string} params.description - Transfer description/content
 * @param {string} params.template - QR template style (default: 'compact2')
 * @returns {string} VietQR image URL
 *
 * @example
 * const qrUrl = generateVietQR({
 *   bankBin: BANK_BINS.VIETCOMBANK,
 *   accountNo: '1234567890',
 *   accountName: 'APTECHMALL COMPANY',
 *   amount: 500000,
 *   description: 'NAP TIEN USER123',
 *   template: QR_TEMPLATES.COMPACT2
 * });
 */
export const generateVietQR = ({
  bankBin,
  accountNo,
  accountName,
  amount,
  description,
  template = QR_TEMPLATES.COMPACT2
}) => {
  // Base URL for VietQR API
  const baseUrl = 'https://img.vietqr.io/image';

  // Build URL: {BIN}-{ACCOUNT_NO}-{TEMPLATE}.png
  let url = `${baseUrl}/${bankBin}-${accountNo}-${template}.png`;

  // Add query parameters
  const params = new URLSearchParams();

  if (amount && amount > 0) {
    params.append('amount', amount.toString());
  }

  if (description) {
    params.append('addInfo', description);
  }

  if (accountName) {
    params.append('accountName', accountName);
  }

  const queryString = params.toString();
  if (queryString) {
    url += `?${queryString}`;
  }

  return url;
};

/**
 * Get bank name from BIN code
 * @param {string} bin - Bank BIN code
 * @returns {string} Bank name
 */
export const getBankName = (bin) => {
  const bankMap = {
    [BANK_BINS.VIETCOMBANK]: 'Vietcombank',
    [BANK_BINS.TECHCOMBANK]: 'Techcombank',
    [BANK_BINS.BIDV]: 'BIDV',
    [BANK_BINS.AGRIBANK]: 'Agribank',
    [BANK_BINS.MBBANK]: 'MB Bank',
    [BANK_BINS.VIETINBANK]: 'VietinBank',
    [BANK_BINS.SACOMBANK]: 'Sacombank',
    [BANK_BINS.ACB]: 'ACB',
    [BANK_BINS.VPBANK]: 'VPBank',
    [BANK_BINS.TPBANK]: 'TPBank',
    [BANK_BINS.HDBANK]: 'HDBank',
    [BANK_BINS.SHB]: 'SHB',
    [BANK_BINS.OCB]: 'OCB',
    [BANK_BINS.MSB]: 'MSB',
    [BANK_BINS.SEABANK]: 'SeABank'
  };

  return bankMap[bin] || 'Unknown Bank';
};

/**
 * Download QR code image
 * @param {string} qrUrl - QR code image URL
 * @param {string} filename - Download filename (default: 'bank-transfer-qr.png')
 */
export const downloadQR = async (qrUrl, filename = 'bank-transfer-qr.png') => {
  try {
    const response = await fetch(qrUrl);
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Error downloading QR code:', error);
    throw new Error('Failed to download QR code');
  }
};

/**
 * Validate bank account configuration
 * @param {Object} config - Bank account config
 * @returns {boolean} True if valid
 */
export const validateBankConfig = (config) => {
  if (!config.bankBin || !config.accountNo) {
    return false;
  }

  // Validate BIN (should be 6 digits)
  if (!/^\d{6}$/.test(config.bankBin)) {
    return false;
  }

  // Validate account number (should be numeric)
  if (!/^\d+$/.test(config.accountNo)) {
    return false;
  }

  return true;
};
