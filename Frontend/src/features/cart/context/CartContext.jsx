import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { getCart } from '../services';
import { useAuth } from '../../auth/context/AuthContext';

const CartContext = createContext();

export const CartProvider = ({ children }) => {
  const { user } = useAuth(); // Get user from AuthContext
  const [cartCount, setCartCount] = useState(0);
  const [loading, setLoading] = useState(false);

  // Fetch cart count
  // User ID is automatically extracted from JWT token by backend
  const fetchCartCount = useCallback(async () => {
    try {
      setLoading(true);
      console.log('🔄 [CartContext] Fetching cart count...');
      const cart = await getCart(); // No userId needed - extracted from JWT token
      console.log('📦 [CartContext] Cart data:', cart);
      const count = cart.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
      console.log('🔢 [CartContext] Cart count:', count);
      setCartCount(count);
    } catch (error) {
      console.error('❌ [CartContext] Error fetching cart count:', error);
      setCartCount(0);
    } finally {
      setLoading(false);
    }
  }, []);

  // Refresh cart count (called after cart modifications)
  const refreshCart = useCallback(() => {
    fetchCartCount();
  }, [fetchCartCount]);

  // Initial fetch on mount - only once
  useEffect(() => {
    fetchCartCount();
  }, []); // Empty array - only run once on mount

  // Refresh cart count when user logs in or out
  useEffect(() => {
    console.log('👤 [CartContext] User state changed:', user ? 'Logged in' : 'Logged out');
    if (user) {
      // User just logged in - refresh cart count
      console.log('🔄 [CartContext] User logged in - refreshing cart count');
      fetchCartCount();
    } else {
      // User logged out - reset cart count
      console.log('🔄 [CartContext] User logged out - resetting cart count');
      setCartCount(0);
    }
  }, [user, fetchCartCount]); // Re-run when user changes

  // Memoize value to prevent unnecessary re-renders
  const value = useMemo(() => ({
    cartCount,
    loading,
    refreshCart
  }), [cartCount, loading, refreshCart]);

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
};

// Custom hook to use cart context
export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within CartProvider');
  }
  return context;
};
