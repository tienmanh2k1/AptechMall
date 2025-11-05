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
import AdminShopManagementPage from './features/admin/pages/AdminShopManagementPage';
import AdminUserManagementPage from './features/admin/pages/AdminUserManagementPage';

const HomePage = () => (
  <div className="container mx-auto px-4 py-16 text-center">
    <h1 className="text-4xl font-bold mb-4">Welcome to PandaMall</h1>
    <p className="text-gray-600 mb-8">Your one-stop shop for quality products</p>
    <div className="flex flex-col sm:flex-row gap-4 justify-center">
      <Link
        to="/search"
        className="inline-block px-6 py-3 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
      >
        Search Products
      </Link>
      <Link
        to="/aliexpress/products/1005005244562338"
        className="inline-block px-6 py-3 bg-gray-200 text-gray-800 rounded-lg font-medium hover:bg-gray-300 transition-colors"
      >
        View Sample Product
      </Link>
    </div>
  </div>
);

const NotFoundPage = () => (
  <div className="container mx-auto px-4 py-16 text-center">
    <h1 className="text-4xl font-bold mb-4">404 - Page Not Found</h1>
    <p className="text-gray-600 mb-8">The page you are looking for does not exist.</p>
    <Link
      to="/"
      className="inline-block px-6 py-3 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
    >
      Go Home
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

              {/* Admin routes - Require admin role */}
              <Route
                path="/admin/shops"
                element={
                  <AdminRoute>
                    <AdminShopManagementPage />
                  </AdminRoute>
                }
              />

              <Route
                path="/admin/users"
                element={
                  <AdminRoute>
                    <AdminUserManagementPage />
                  </AdminRoute>
                }
              />

              {/* 404 Page */}
              <Route path="*" element={<NotFoundPage />} />
            </Route>
            </Routes>
          </CartProvider>
        </CurrencyProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;