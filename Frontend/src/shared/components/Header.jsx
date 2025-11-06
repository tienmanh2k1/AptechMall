import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, User, Search, Package, LogOut, LogIn, UserPlus, Wallet } from 'lucide-react';
import { toast } from 'react-toastify';
import { useCart } from '../../features/cart/context/CartContext';
import { useAuth } from '../../features/auth/context/AuthContext';

const Header = () => {
  const { cartCount } = useCart();
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const menuRef = useRef(null);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowUserMenu(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    setShowUserMenu(false);
    toast.success('Logged out successfully');
    navigate('/');
  };

  return (
    <header className="bg-white shadow-sm sticky top-0 z-50">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="flex items-center space-x-2">
            <span className="text-2xl">🐼</span>
            <span className="text-xl font-bold text-gray-900">AptechMall</span>
          </Link>

          <nav className="hidden md:flex items-center space-x-6">
            <Link to="/search" className="text-gray-700 hover:text-red-600 flex items-center gap-2">
              <Search className="w-5 h-5" />
              <span>Search</span>
            </Link>
            {isAuthenticated() && (
              <Link to="/wallet" className="text-gray-700 hover:text-red-600 flex items-center gap-2">
                <Wallet className="w-5 h-5" />
                <span>Wallet</span>
              </Link>
            )}
            <Link to="/orders" className="text-gray-700 hover:text-red-600 flex items-center gap-2">
              <Package className="w-5 h-5" />
              <span>Orders</span>
            </Link>
            <Link to="/cart" className="text-gray-700 hover:text-red-600">
              Cart
            </Link>
          </nav>

          <div className="flex items-center space-x-6">
            <Link to="/cart" className="relative text-gray-700 hover:text-red-600">
              <ShoppingCart className="w-6 h-6" />
              {cartCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-red-600 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {cartCount > 99 ? '99+' : cartCount}
                </span>
              )}
            </Link>

            {/* Auth Section */}
            {isAuthenticated() ? (
              <div className="relative" ref={menuRef}>
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="flex items-center gap-2 text-gray-700 hover:text-red-600"
                >
                  <User className="w-6 h-6" />
                  <span className="hidden md:inline text-sm font-medium">{user?.name}</span>
                </button>

                {/* User Dropdown Menu */}
                {showUserMenu && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg py-2 border border-gray-200">
                    <div className="px-4 py-2 border-b border-gray-200">
                      <p className="text-sm font-medium text-gray-900">{user?.name}</p>
                      <p className="text-xs text-gray-500">{user?.email}</p>
                    </div>
                    <Link
                      to="/wallet"
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
                      onClick={() => setShowUserMenu(false)}
                    >
                      <Wallet className="w-4 h-4" />
                      My Wallet
                    </Link>
                    <Link
                      to="/orders"
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
                      onClick={() => setShowUserMenu(false)}
                    >
                      <Package className="w-4 h-4" />
                      My Orders
                    </Link>
                    <button
                      onClick={handleLogout}
                      className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100 flex items-center gap-2"
                    >
                      <LogOut className="w-4 h-4" />
                      Logout
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/login"
                  className="flex items-center gap-1 text-gray-700 hover:text-red-600 text-sm font-medium"
                >
                  <LogIn className="w-5 h-5" />
                  <span className="hidden md:inline">Login</span>
                </Link>
                <Link
                  to="/register"
                  className="flex items-center gap-1 px-4 py-2 bg-gradient-to-r from-orange-500 to-red-600 text-white rounded-lg hover:from-orange-600 hover:to-red-700 transition-all text-sm font-medium"
                >
                  <UserPlus className="w-4 h-4" />
                  <span className="hidden md:inline">Register</span>
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;