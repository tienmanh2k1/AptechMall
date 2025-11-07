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
    </div>
  );
};

export default AdminHomePage;
