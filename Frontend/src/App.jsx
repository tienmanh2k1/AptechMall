import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { AuthProvider } from './features/auth/context/AuthContext';
import { CurrencyProvider } from './features/currency/context/CurrencyContext';
import { CartProvider } from './features/cart/context/CartContext';
import ProtectedRoute from './features/auth/components/ProtectedRoute';
import Layout from './shared/components/Layout';
import LoginPage from './features/auth/pages/LoginPage';
import RegisterPage from './features/auth/pages/RegisterPage';
import ProductDetailPage from './features/product/pages/ProductDetailPage';
import SearchPage from './features/product/pages/SearchPage';
import CartPage from './features/cart/pages/CartPage';
import CheckoutPage from './features/order/pages/CheckoutPage';
import CheckoutSuccessPage from './features/order/pages/CheckoutSuccessPage';
import OrderListPage from './features/order/pages/OrderListPage';
import OrderDetailPage from './features/order/pages/OrderDetailPage';
import CartDebugPage from './features/cart/pages/CartDebugPage';
import AdminRoute from './features/admin/components/AdminRoute';
import AdminLayout from './features/admin/components/AdminLayout';
import AdminLoginPage from './features/admin/pages/AdminLoginPage';
import AdminHomePage from './features/admin/pages/AdminHomePage';
import AdminUserManagementPage from './features/admin/pages/AdminUserManagementPage';
import AdminWalletManagementPage from './features/admin/pages/AdminWalletManagementPage';
import AdminDashboardPage from './features/admin/pages/AdminDashboardPage';
import AdminOrderManagementPage from './features/admin/pages/AdminOrderManagementPage';
import AdminOrderDetailPage from './features/admin/pages/AdminOrderDetailPage';
import WalletPage from './features/wallet/pages/WalletPage';
import BankTransferDepositPage from './features/wallet/pages/BankTransferDepositPage';
import TransactionHistoryPage from './features/wallet/pages/TransactionHistoryPage';
import ProfilePage from './features/user/pages/ProfilePage';
import AddressManagementPage from './features/user/pages/AddressManagementPage';

const HomePage = () => (
  <div className="container mx-auto px-4 py-16 text-center">
    <h1 className="text-4xl font-bold mb-4">Chào mừng đến với AptechMall</h1>
    <p className="text-gray-600 mb-8">Nền tảng mua sắm trực tuyến uy tín</p>
    <div className="flex flex-col sm:flex-row gap-4 justify-center">
      <Link
        to="/search"
        className="inline-block px-6 py-3 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
      >
        Tìm kiếm sản phẩm
      </Link>
      <Link
        to="/aliexpress/products/1005005244562338"
        className="inline-block px-6 py-3 bg-gray-200 text-gray-800 rounded-lg font-medium hover:bg-gray-300 transition-colors"
      >
        Xem sản phẩm mẫu
      </Link>
    </div>

    {/* Admin Login Link */}
    <div className="mt-12 pt-8 border-t border-gray-200">
      <p className="text-sm text-gray-500 mb-4">Bạn là quản trị viên hoặc nhân viên?</p>
      <Link
        to="/admin/login"
        className="inline-flex items-center gap-2 px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
        Cổng quản trị
      </Link>
    </div>
  </div>
);

const NotFoundPage = () => (
  <div className="container mx-auto px-4 py-16 text-center">
    <h1 className="text-4xl font-bold mb-4">404 - Không tìm thấy trang</h1>
    <p className="text-gray-600 mb-8">Trang bạn đang tìm kiếm không tồn tại.</p>
    <Link
      to="/"
      className="inline-block px-6 py-3 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
    >
      Về trang chủ
    </Link>
  </div>
);

function App() {
  return (
    <Router>
      <AuthProvider>
        <CurrencyProvider>
          <CartProvider>
            <Routes>
            {/* Public routes - No Layout */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/admin/login" element={<AdminLoginPage />} />

            {/* Public routes - With Layout */}
            <Route element={<Layout />}>
              <Route path="/" element={<HomePage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/:platform/products/:id" element={<ProductDetailPage />} />

              {/* Protected routes - Require authentication */}
              <Route
                path="/cart"
                element={
                  <ProtectedRoute>
                    <CartPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/checkout"
                element={
                  <ProtectedRoute>
                    <CheckoutPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders/success"
                element={
                  <ProtectedRoute>
                    <CheckoutSuccessPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders"
                element={
                  <ProtectedRoute>
                    <OrderListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders/:orderId"
                element={
                  <ProtectedRoute>
                    <OrderDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cart-debug"
                element={
                  <ProtectedRoute>
                    <CartDebugPage />
                  </ProtectedRoute>
                }
              />

              {/* User Profile - Require authentication */}
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* User Addresses - Require authentication */}
              <Route
                path="/addresses"
                element={
                  <ProtectedRoute>
                    <AddressManagementPage />
                  </ProtectedRoute>
                }
              />

              {/* Wallet routes - Require authentication */}
              <Route
                path="/wallet"
                element={
                  <ProtectedRoute>
                    <WalletPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wallet/deposit/bank-transfer"
                element={
                  <ProtectedRoute>
                    <BankTransferDepositPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wallet/transactions"
                element={
                  <ProtectedRoute>
                    <TransactionHistoryPage />
                  </ProtectedRoute>
                }
              />

              {/* 404 Page */}
              <Route path="*" element={<NotFoundPage />} />
            </Route>

            {/* Admin routes - Separate layout with AdminRoute protection */}
            <Route
              element={
                <AdminRoute>
                  <AdminLayout />
                </AdminRoute>
              }
            >
              <Route path="/admin" element={<AdminHomePage />} />
              <Route path="/admin/home" element={<AdminHomePage />} />
              <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
              <Route path="/admin/orders" element={<AdminOrderManagementPage />} />
              <Route path="/admin/orders/:orderId" element={<AdminOrderDetailPage />} />
              <Route path="/admin/users" element={<AdminUserManagementPage />} />
              <Route path="/admin/wallets" element={<AdminWalletManagementPage />} />
            </Route>
            </Routes>
          </CartProvider>
        </CurrencyProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;