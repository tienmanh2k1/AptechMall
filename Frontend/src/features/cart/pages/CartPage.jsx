import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { getCart, updateCartItem, removeCartItem } from '../services';
import { useCart } from '../context/CartContext';
import CartItem from '../components/CartItem';
import CartSummary from '../components/CartSummary';
import CartEmpty from '../components/CartEmpty';
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';

const CartPage = () => {
  const [cart, setCart] = useState({ items: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updatingItemId, setUpdatingItemId] = useState(null);
  const [selectedItems, setSelectedItems] = useState(new Set());
  const { refreshCart } = useCart();

  // Fetch cart data
  // User ID is automatically extracted from JWT token by backend
  const fetchCart = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getCart(); // No userId needed

      // Debug: Log backend response
      console.log('🛒 [CartPage] Backend response:', data);

      // Normalize backend response:
      // 1. Ensure items array exists
      // 2. Map backend field names to frontend field names
      const normalizedCart = {
        userId: data?.userId, // userId from backend response
        items: Array.isArray(data?.items)
          ? data.items.map(item => ({
              ...item,
              // Map backend fields to frontend fields
              title: item.productName || item.title,           // productName → title
              image: item.productImage || item.image,          // productImage → image
              platform: item.marketplace?.toLowerCase() || item.platform, // ALIEXPRESS → aliexpress
              // Keep original fields for backward compatibility
              productName: item.productName,
              productImage: item.productImage,
              marketplace: item.marketplace
            }))
          : []
      };

      console.log('✅ [CartPage] Normalized cart:', normalizedCart);

      // Debug: Log first item to check field names
      if (normalizedCart.items.length > 0) {
        console.log('🔍 [CartPage] First item fields:', {
          hasImage: !!normalizedCart.items[0].image,
          hasProductImage: !!normalizedCart.items[0].productImage,
          imageValue: normalizedCart.items[0].image,
          title: normalizedCart.items[0].title,
          platform: normalizedCart.items[0].platform,
          allFields: Object.keys(normalizedCart.items[0])
        });
      }

      setCart(normalizedCart);
    } catch (err) {
      console.error('❌ [CartPage] Error fetching cart:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load cart');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCart();
  }, []);

  // Handle quantity update
  const handleUpdateQuantity = async (itemId, newQuantity) => {
    try {
      setUpdatingItemId(itemId);
      await updateCartItem(itemId, newQuantity); // No userId needed

      // Update local state
      setCart(prevCart => ({
        ...prevCart,
        items: prevCart.items.map(item =>
          item.id === itemId ? { ...item, quantity: newQuantity } : item
        )
      }));

      // Refresh cart count in header
      refreshCart();
      toast.success('Cart updated successfully');
    } catch (err) {
      console.error('Error updating cart item:', err);
      toast.error(err.response?.data?.message || 'Failed to update cart');
    } finally {
      setUpdatingItemId(null);
    }
  };

  // Handle item removal
  const handleRemoveItem = async (itemId) => {
    try {
      setUpdatingItemId(itemId);
      await removeCartItem(itemId); // No userId needed

      // Update local state
      setCart(prevCart => ({
        ...prevCart,
        items: prevCart.items.filter(item => item.id !== itemId)
      }));

      // Remove from selected items
      setSelectedItems(prev => {
        const newSet = new Set(prev);
        newSet.delete(itemId);
        return newSet;
      });

      // Refresh cart count in header
      refreshCart();
      toast.success('Item removed from cart');
    } catch (err) {
      console.error('Error removing cart item:', err);
      toast.error(err.response?.data?.message || 'Failed to remove item');
    } finally {
      setUpdatingItemId(null);
    }
  };

  // Handle item selection
  const handleSelectItem = (itemId, checked) => {
    setSelectedItems(prev => {
      const newSet = new Set(prev);
      if (checked) {
        newSet.add(itemId);
      } else {
        newSet.delete(itemId);
      }
      return newSet;
    });
  };

  // Handle select all
  const handleSelectAll = (checked) => {
    if (checked) {
      setSelectedItems(new Set(cart?.items?.map(item => item.id) || []));
    } else {
      setSelectedItems(new Set());
    }
  };

  const allSelected = cart?.items?.length > 0 && selectedItems.size === cart.items.length;

  // Loading state
  if (loading) {
    return <Loading message="Loading your cart..." />;
  }

  // Error state
  if (error) {
    return (
      <div className="container mx-auto px-4 py-16">
        <ErrorMessage
          message={error}
          onRetry={fetchCart}
        />
      </div>
    );
  }

  // Empty cart
  if (!cart.items || cart.items.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16">
        <CartEmpty />
      </div>
    );
  }

  // Cart with items
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Giỏ hàng</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Cart Items */}
        <div className="lg:col-span-2 space-y-4">
          {/* Select All Header */}
          <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-3 flex items-center gap-3">
            <input
              type="checkbox"
              checked={allSelected}
              onChange={(e) => handleSelectAll(e.target.checked)}
              className="w-5 h-5 text-orange-500 border-gray-300 rounded focus:ring-orange-500 cursor-pointer"
            />
            <span className="text-sm font-medium text-gray-700">
              Chọn tất cả ({cart.items.length} sản phẩm)
            </span>
            <span className="text-sm text-gray-500 ml-auto">
              Đã chọn: {selectedItems.size}
            </span>
          </div>

          {/* Cart Items List */}
          {cart.items.map((item) => (
            <CartItem
              key={item.id}
              item={item}
              selected={selectedItems.has(item.id)}
              onSelect={handleSelectItem}
              onUpdateQuantity={handleUpdateQuantity}
              onRemove={handleRemoveItem}
              loading={updatingItemId === item.id}
            />
          ))}
        </div>

        {/* Cart Summary */}
        <div className="lg:col-span-1">
          <CartSummary
            cart={cart}
            selectedItems={selectedItems}
            loading={updatingItemId !== null}
          />
        </div>
      </div>
    </div>
  );
};

export default CartPage;
