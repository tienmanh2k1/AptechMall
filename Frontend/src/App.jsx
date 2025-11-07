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

// 🏠 Trang chủ (thiết kế chuyên nghiệp)
const HomePage = () => (
  <div className="bg-white text-gray-800">
    {/* HERO SECTION */}
    <section className="min-h-[80vh] flex flex-col justify-center items-center text-center px-6 py-20 bg-gradient-to-b from-[#fff5f5] to-white">
      <h1 className="text-5xl sm:text-6xl font-extrabold mb-6 bg-clip-text text-transparent bg-gradient-to-r from-[#FF3B30] to-[#FF6F61] leading-tight">
        Mua Sắm Thông Minh, Sống Tốt Hơn
      </h1>
      <p className="text-lg text-gray-600 mb-8 max-w-2xl">
        AptechMall kết nối bạn với các nhà bán hàng toàn cầu uy tín và sản phẩm
        chất lượng cao cho mọi phong cách sống. Khám phá chất lượng, tiện nghi và giá trị tốt nhất.
      </p>
      <div className="flex flex-col sm:flex-row gap-4">
        <Link
          to="/search"
          className="px-8 py-3 bg-[#FF3B30] text-white rounded-full font-semibold hover:scale-105 shadow-md hover:shadow-lg transition-all"
        >
          🛍️ Khám Phá Sản Phẩm
        </Link>
        <Link
          to="/register"
          className="px-8 py-3 bg-white border border-gray-300 text-gray-700 rounded-full font-semibold hover:bg-gray-50 hover:scale-105 transition-all"
        >
          ✨ Bắt Đầu Ngay
        </Link>
      </div>
    </section>

    {/* WHY CHOOSE US */}
    <section className="py-20 px-6 bg-white text-center">
      <h2 className="text-3xl font-bold mb-12">Tại Sao Khách Hàng Yêu Thích AptechMall</h2>
      <div className="grid sm:grid-cols-3 gap-8 max-w-6xl mx-auto">
        {[
          {
            title: "Giao Hàng Nhanh & An Toàn",
            desc: "Vận chuyển có theo dõi và đóng gói cẩn thận để bạn an tâm.",
            icon: "🚚",
          },
          {
            title: "Thương Hiệu Toàn Cầu Uy Tín",
            desc: "Khám phá sản phẩm chất lượng cao từ các nhà bán hàng đáng tin cậy trên toàn thế giới.",
            icon: "🌏",
          },
          {
            title: "Chính Sách Ưu Tiên Khách Hàng",
            desc: "Hoàn tiền, đổi trả và hỗ trợ nhanh chóng bất cứ khi nào bạn cần.",
            icon: "🤝",
          },
        ].map((item, i) => (
          <div
            key={i}
            className="p-8 rounded-2xl border border-gray-100 hover:border-[#FF3B30]/30 shadow-sm hover:shadow-md transition-all"
          >
            <div className="text-4xl mb-4">{item.icon}</div>
            <h3 className="text-xl font-semibold mb-2">{item.title}</h3>
            <p className="text-gray-600 text-sm">{item.desc}</p>
          </div>
        ))}
      </div>
    </section>


    {/* CUSTOMER REVIEWS */}
    <section className="py-20 px-6 bg-white text-center">
      <h2 className="text-3xl font-bold mb-8 text-gray-800">
        Khách Hàng Nói Gì Về Chúng Tôi
      </h2>
      <div className="grid sm:grid-cols-3 gap-8 max-w-5xl mx-auto">
        {[
          {
            name: "Emily Nguyễn",
            text: "Tôi đã tìm được đúng thứ mình muốn với giá cả hợp lý. Giao hàng rất nhanh!",
            avatar: "https://randomuser.me/api/portraits/women/79.jpg",
          },
          {
            name: "David Trần",
            text: "Trải nghiệm mượt mà và người bán đáng tin cậy. Sẽ quay lại mua hàng!",
            avatar: "https://randomuser.me/api/portraits/men/32.jpg",
          },
          {
            name: "Linh Phạm",
            text: "Hỗ trợ khách hàng rất nhanh chóng và nhiệt tình!",
            avatar: "https://randomuser.me/api/portraits/women/44.jpg",
          },
        ].map((r, i) => (
          <div
            key={i}
            className="p-6 bg-[#fff8f8] rounded-2xl shadow-sm hover:shadow-md transition-all"
          >
            <img
              src={r.avatar}
              alt={r.name}
              className="w-16 h-16 rounded-full mx-auto mb-4 object-cover"
            />
            <p className="text-gray-600 italic mb-3">"{r.text}"</p>
            <h4 className="text-gray-800 font-semibold">{r.name}</h4>
          </div>
        ))}
      </div>
    </section>

    {/* CALL TO ACTION */}
    <section className="py-16 bg-[#FF3B30] text-white text-center">
      <h2 className="text-3xl font-bold mb-4">Bắt Đầu Mua Sắm Thông Minh Ngay Hôm Nay!</h2>
      <p className="text-white/90 mb-8">
        Tham gia cùng hàng nghìn khách hàng hài lòng tin tưởng AptechMall.
      </p>
      <Link
        to="/register"
        className="bg-white text-[#FF3B30] px-8 py-3 rounded-full font-semibold hover:bg-gray-100 transition-colors"
      >
        Tạo Tài Khoản →
      </Link>
    </section>

  </div>
);

// 404 PAGE
const NotFoundPage = () => (
  <div className="min-h-[80vh] flex flex-col items-center justify-center text-center px-6">
    <img
      src="https://illustrations.popsy.co/gray/error-404.svg"
      alt="404"
      className="w-72 mb-6 opacity-90"
    />
    <h1 className="text-4xl font-bold text-gray-800 mb-3">
      Oops! Không Tìm Thấy Trang
    </h1>
    <p className="text-gray-600 mb-6 max-w-sm">
      Trang bạn đang tìm kiếm không tồn tại hoặc đã được di chuyển.
    </p>
    <Link
      to="/"
      className="px-6 py-3 bg-[#FF3B30] text-white rounded-full font-medium hover:bg-[#ff564d] transition-colors"
    >
      Về Trang Chủ
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