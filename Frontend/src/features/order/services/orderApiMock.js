/**
 * Mock Order API Service
 * Use this temporarily if backend order endpoints are not ready
 * Replace with real orderApi.js when backend is ready
 */

// Mock in-memory order storage
let mockOrders = [];
let nextOrderId = 1;
let nextOrderNumber = 1000;

// Simulate API delay
const delay = (ms = 500) => new Promise(resolve => setTimeout(resolve, ms));

/**
 * Create an order (checkout)
 */
export const checkout = async (userId, checkoutData) => {
  await delay(500);
  console.log('[MOCK] Creating order:', checkoutData);

  // Get cart items from cartApiMock
  // In real app, backend will get items from cart on server side
  let cartItems = [];
  try {
    // Import cart mock to get current cart
    const { getCart } = await import('../../cart/services/cartApiMock.js');
    const cart = await getCart(userId);
    cartItems = cart.items || [];
  } catch (err) {
    console.warn('[MOCK] Could not get cart items:', err);
  }

  const newOrder = {
    id: nextOrderId++,
    orderNumber: nextOrderNumber++,
    userId: userId,
    status: 'PENDING',
    shippingAddress: checkoutData.shippingAddress,
    phone: checkoutData.phone,
    note: checkoutData.note || '',
    items: cartItems,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  mockOrders.push(newOrder);

  // Clear cart after checkout
  try {
    const { clearCart } = await import('../../cart/services/cartApiMock.js');
    await clearCart(userId);
  } catch (err) {
    console.warn('[MOCK] Could not clear cart:', err);
  }

  return newOrder;
};

/**
 * Get user's orders with pagination
 */
export const getOrders = async (userId, page = 1, size = 10, status = null) => {
  await delay(300);
  console.log('[MOCK] Getting orders:', { userId, page, size, status });

  let filteredOrders = mockOrders.filter(o => o.userId === userId);

  if (status) {
    filteredOrders = filteredOrders.filter(o => o.status === status);
  }

  // Sort by created date desc
  filteredOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  // Pagination
  const start = (page - 1) * size;
  const end = start + size;
  const paginatedOrders = filteredOrders.slice(start, end);

  return {
    orders: paginatedOrders,
    totalPages: Math.ceil(filteredOrders.length / size),
    currentPage: page,
    totalElements: filteredOrders.length
  };
};

/**
 * Get order by ID
 */
export const getOrderById = async (userId, orderId) => {
  await delay(300);
  console.log('[MOCK] Getting order:', orderId);

  const order = mockOrders.find(o => o.id === parseInt(orderId) && o.userId === userId);

  if (!order) {
    throw new Error('Order not found');
  }

  return order;
};

/**
 * Cancel order
 */
export const cancelOrder = async (userId, orderId) => {
  await delay(500);
  console.log('[MOCK] Cancelling order:', orderId);

  const order = mockOrders.find(o => o.id === parseInt(orderId) && o.userId === userId);

  if (!order) {
    throw new Error('Order not found');
  }

  if (order.status !== 'PENDING') {
    throw new Error('Only pending orders can be cancelled');
  }

  order.status = 'CANCELLED';
  order.updatedAt = new Date().toISOString();

  return order;
};

/**
 * Update order status (admin only in real app)
 */
export const updateOrderStatus = async (userId, orderId, status) => {
  await delay(300);
  console.log('[MOCK] Updating order status:', orderId, status);

  const order = mockOrders.find(o => o.id === parseInt(orderId));

  if (!order) {
    throw new Error('Order not found');
  }

  order.status = status;
  order.updatedAt = new Date().toISOString();

  return order;
};

// Helper: Add sample orders for testing
export const addSampleOrders = (userId) => {
  mockOrders = [
    {
      id: nextOrderId++,
      orderNumber: nextOrderNumber++,
      userId: userId,
      status: 'DELIVERED',
      shippingAddress: '123 Main St, City, Country',
      phone: '+1234567890',
      note: 'Please deliver in the morning',
      items: [
        {
          id: 1,
          productId: '1005005244562338',
          platform: 'aliexpress',
          title: 'Sample Product 1',
          price: 29.99,
          currency: 'USD',
          quantity: 2,
          image: 'https://via.placeholder.com/100'
        }
      ],
      createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString()
    },
    {
      id: nextOrderId++,
      orderNumber: nextOrderNumber++,
      userId: userId,
      status: 'SHIPPING',
      shippingAddress: '456 Oak Ave, City, Country',
      phone: '+1234567890',
      note: '',
      items: [
        {
          id: 2,
          productId: '123456',
          platform: '1688',
          title: 'Sample Product 2',
          price: 199,
          currency: 'CNY',
          quantity: 1,
          image: 'https://via.placeholder.com/100'
        }
      ],
      createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString()
    },
    {
      id: nextOrderId++,
      orderNumber: nextOrderNumber++,
      userId: userId,
      status: 'PENDING',
      shippingAddress: '789 Pine Rd, City, Country',
      phone: '+1234567890',
      note: '',
      items: [
        {
          id: 3,
          productId: '789012',
          platform: '1688',
          title: 'Sample Product 3',
          price: 88,
          currency: 'CNY',
          quantity: 5,
          image: 'https://via.placeholder.com/100'
        }
      ],
      createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
      updatedAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString()
    }
  ];
};
