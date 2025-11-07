import React, { useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/context/AuthContext';
import {
  LayoutDashboard,
  ShoppingCart,
  Users,
  Wallet,
  LogOut,
  Menu,
  X,
  ChevronRight,
  Home,
} from 'lucide-react';

const AdminLayout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const handleLogout = () => {
    logout();
    navigate('/admin/login');
  };

  // Define all menu items with role restrictions
  const allMenuItems = [
    {
      name: 'Home',
      path: '/admin',
      icon: Home,
      description: 'Admin Portal Home',
      roles: ['ADMIN', 'STAFF'] // Both can access
    },
    {
      name: 'Dashboard',
      path: '/admin/dashboard',
      icon: LayoutDashboard,
      description: 'Statistics & Analytics',
      roles: ['ADMIN'] // Admin only
    },
    {
      name: 'Orders',
      path: '/admin/orders',
      icon: ShoppingCart,
      description: 'Manage Orders',
      roles: ['ADMIN', 'STAFF'] // Both can access
    },
    {
      name: 'Users',
      path: '/admin/users',
      icon: Users,
      description: 'User Management',
      roles: ['ADMIN'] // Admin only
    },
    {
      name: 'Wallets',
      path: '/admin/wallets',
      icon: Wallet,
      description: 'Wallet Management',
      roles: ['ADMIN'] // Admin only
    },
  ];

  // Filter menu items based on user role
  const menuItems = allMenuItems.filter(item =>
    item.roles.includes(user?.role)
  );

  const isActive = (path) => location.pathname === path;

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside
        className={`${
          sidebarOpen ? 'w-64' : 'w-20'
        } bg-gray-900 text-white transition-all duration-300 flex flex-col fixed h-full z-30`}
      >
        {/* Sidebar Header */}
        <div className="p-4 border-b border-gray-800">
          <div className="flex items-center justify-between">
            {sidebarOpen && (
              <div>
                <h1 className="text-xl font-bold text-red-500">AptechMall</h1>
                <p className="text-xs text-gray-400">
                  {user?.role === 'STAFF' ? 'Cổng nhân viên' : 'Cổng quản trị'}
                </p>
              </div>
            )}
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-2 rounded-lg hover:bg-gray-800 transition-colors"
            >
              {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* User Info */}
        <div className="p-4 border-b border-gray-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-red-600 flex items-center justify-center font-bold">
              {user?.fullname?.charAt(0) || user?.email?.charAt(0) || 'A'}
            </div>
            {sidebarOpen && (
              <div className="flex-1 overflow-hidden">
                <p className="text-sm font-medium truncate">{user?.fullname || 'Admin User'}</p>
                <p className="text-xs text-gray-400 truncate">{user?.role}</p>
              </div>
            )}
          </div>
        </div>

        {/* Navigation Menu */}
        <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);

            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-3 rounded-lg transition-colors group ${
                  active
                    ? 'bg-red-600 text-white'
                    : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                }`}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                {sidebarOpen && (
                  <>
                    <div className="flex-1">
                      <p className="text-sm font-medium">{item.name}</p>
                      <p className="text-xs opacity-75">{item.description}</p>
                    </div>
                    {active && <ChevronRight className="w-4 h-4" />}
                  </>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Logout Button */}
        <div className="p-4 border-t border-gray-800">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-3 rounded-lg text-gray-300 hover:bg-red-600 hover:text-white transition-colors w-full"
          >
            <LogOut className="w-5 h-5" />
            {sidebarOpen && <span className="text-sm font-medium">Logout</span>}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className={`flex-1 ${sidebarOpen ? 'ml-64' : 'ml-20'} transition-all duration-300`}>
        {/* Top Bar */}
        <header className="bg-white border-b border-gray-200 sticky top-0 z-20">
          <div className="px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">
                  {menuItems.find(item => item.path === location.pathname)?.name || 'Admin Panel'}
                </h2>
                <p className="text-sm text-gray-600">
                  {menuItems.find(item => item.path === location.pathname)?.description || 'Management Portal'}
                </p>
              </div>

              {/* Quick Actions */}
              <div className="flex items-center gap-4">
                <Link
                  to="/"
                  className="text-sm text-gray-600 hover:text-gray-900 transition-colors"
                >
                  ← Back to Customer Portal
                </Link>
              </div>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="p-6">
          <Outlet />
        </main>

        {/* Footer */}
        <footer className="bg-white border-t border-gray-200 px-6 py-4 mt-8">
          <div className="flex items-center justify-between text-sm text-gray-600">
            <p>© 2025 AptechMall {user?.role === 'STAFF' ? 'Cổng nhân viên' : 'Cổng quản trị'}</p>
            <p>Đăng nhập: <span className="font-medium">{user?.email}</span> ({user?.role})</p>
          </div>
        </footer>
      </div>
    </div>
  );
};

export default AdminLayout;
