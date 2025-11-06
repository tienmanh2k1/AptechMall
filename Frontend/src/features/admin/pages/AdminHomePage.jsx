import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/context/AuthContext';
import {
  LayoutDashboard,
  ShoppingCart,
  Users,
  Wallet,
  Store,
  Settings,
  TrendingUp,
  BarChart3,
  Shield,
} from 'lucide-react';

const AdminHomePage = () => {
  const { user } = useAuth();

  const allQuickActions = [
    {
      title: 'Dashboard',
      description: 'View statistics and analytics',
      icon: LayoutDashboard,
      path: '/admin/dashboard',
      color: 'bg-blue-500',
      stats: 'Overview & Metrics',
      roles: ['ADMIN'] // Admin only
    },
    {
      title: 'Order Management',
      description: 'Manage customer orders',
      icon: ShoppingCart,
      path: '/admin/orders',
      color: 'bg-green-500',
      stats: 'View & Update Orders',
      roles: ['ADMIN', 'STAFF'] // Both can access
    },
    {
      title: 'User Management',
      description: 'Manage user accounts',
      icon: Users,
      path: '/admin/users',
      color: 'bg-purple-500',
      stats: 'Edit Roles & Permissions',
      roles: ['ADMIN'] // Admin only
    },
    {
      title: 'Wallet Management',
      description: 'Manage user wallets',
      icon: Wallet,
      path: '/admin/wallets',
      color: 'bg-orange-500',
      stats: 'View & Adjust Balances',
      roles: ['ADMIN'] // Admin only
    },
    {
      title: 'Shop Management',
      description: 'Manage shop information',
      icon: Store,
      path: '/admin/shops',
      color: 'bg-pink-500',
      stats: 'Shop Settings',
      roles: ['ADMIN'] // Admin only
    },
    {
      title: 'System Configuration',
      description: 'Configure system fees',
      icon: Settings,
      path: '/admin/fee-config',
      color: 'bg-gray-700',
      stats: 'Fee & Settings',
      roles: ['ADMIN'] // Admin only
    },
  ];

  // Filter quick actions based on user role
  const quickActions = allQuickActions.filter(action =>
    action.roles.includes(user?.role)
  );

  const QuickActionCard = ({ title, description, icon: Icon, path, color, stats }) => (
    <Link
      to={path}
      className="group bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg hover:border-red-500 transition-all duration-300 transform hover:-translate-y-1"
    >
      <div className="flex items-start gap-4">
        <div className={`${color} p-3 rounded-lg text-white group-hover:scale-110 transition-transform`}>
          <Icon className="w-6 h-6" />
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900 mb-1 group-hover:text-red-600 transition-colors">
            {title}
          </h3>
          <p className="text-sm text-gray-600 mb-2">{description}</p>
          <p className="text-xs text-gray-500">{stats}</p>
        </div>
        <div className="opacity-0 group-hover:opacity-100 transition-opacity">
          <svg className="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </Link>
  );

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-red-600 to-red-700 rounded-xl shadow-lg p-8 text-white">
        <div className="flex items-center gap-4 mb-4">
          <div className="p-3 bg-white/20 rounded-lg backdrop-blur-sm">
            <Shield className="w-8 h-8" />
          </div>
          <div>
            <h1 className="text-3xl font-bold mb-2">
              {user?.role === 'STAFF' ? 'Welcome to Staff Portal' : 'Welcome to Admin Portal'}
            </h1>
            <p className="text-red-100">
              {user?.role === 'STAFF'
                ? 'Manage customer orders efficiently'
                : 'Manage your e-commerce platform efficiently'}
            </p>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4">
            <div className="flex items-center gap-3">
              <TrendingUp className="w-8 h-8" />
              <div>
                <p className="text-sm text-red-100">Quick Access</p>
                <p className="text-lg font-semibold">{quickActions.length} Module{quickActions.length > 1 ? 's' : ''}</p>
              </div>
            </div>
          </div>
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4">
            <div className="flex items-center gap-3">
              <BarChart3 className="w-8 h-8" />
              <div>
                <p className="text-sm text-red-100">
                  {user?.role === 'STAFF' ? 'Order Management' : 'Full Control'}
                </p>
                <p className="text-lg font-semibold">
                  {user?.role === 'STAFF' ? 'All Orders' : 'All Features'}
                </p>
              </div>
            </div>
          </div>
          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4">
            <div className="flex items-center gap-3">
              <Shield className="w-8 h-8" />
              <div>
                <p className="text-sm text-red-100">Secure Access</p>
                <p className="text-lg font-semibold">Role-Based</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Quick Actions</h2>
        <p className="text-gray-600 mb-6">Select a module to get started</p>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {quickActions.map((action) => (
            <QuickActionCard key={action.path} {...action} />
          ))}
        </div>
      </div>

      {/* Recent Activity / Tips Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Tips Card */}
        <div className="bg-blue-50 rounded-xl p-6 border border-blue-200">
          <div className="flex items-start gap-3">
            <div className="p-2 bg-blue-500 rounded-lg text-white">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 mb-2">
                {user?.role === 'STAFF' ? 'Staff Tips' : 'Admin Tips'}
              </h3>
              <ul className="space-y-2 text-sm text-gray-700">
                {user?.role === 'STAFF' ? (
                  <>
                    <li>• Review pending orders regularly</li>
                    <li>• Update order status promptly</li>
                    <li>• Add/edit additional services as needed</li>
                    <li>• Contact admin for special cases</li>
                  </>
                ) : (
                  <>
                    <li>• Check Dashboard for daily statistics</li>
                    <li>• Review pending orders regularly</li>
                    <li>• Monitor wallet transactions for issues</li>
                    <li>• Update system fees as needed</li>
                  </>
                )}
              </ul>
            </div>
          </div>
        </div>

        {/* Shortcuts Card */}
        <div className="bg-purple-50 rounded-xl p-6 border border-purple-200">
          <div className="flex items-start gap-3">
            <div className="p-2 bg-purple-500 rounded-lg text-white">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 mb-2">Keyboard Shortcuts</h3>
              <ul className="space-y-2 text-sm text-gray-700">
                {user?.role === 'STAFF' ? (
                  <>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">O</code> - Orders</li>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">H</code> - Home</li>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">Esc</code> - Cancel</li>
                  </>
                ) : (
                  <>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">D</code> - Dashboard</li>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">O</code> - Orders</li>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">U</code> - Users</li>
                    <li>• <code className="bg-purple-100 px-2 py-1 rounded">W</code> - Wallets</li>
                  </>
                )}
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Documentation Link */}
      <div className="bg-gray-100 rounded-xl p-6 text-center">
        <p className="text-gray-700 mb-4">
          Need help? Check out the documentation or contact support
        </p>
        <div className="flex items-center justify-center gap-4">
          <a
            href="#"
            className="inline-flex items-center px-4 py-2 bg-white text-gray-700 rounded-lg hover:bg-gray-50 transition-colors border border-gray-300"
          >
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
            Documentation
          </a>
          <a
            href="#"
            className="inline-flex items-center px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-5 0a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            Contact Support
          </a>
        </div>
      </div>
    </div>
  );
};

export default AdminHomePage;
