import React, { useState } from 'react';
import { getCart, addToCart } from '../services';

const CartDebugPage = () => {
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const testGetCart = async () => {
    try {
      console.log('[DEBUG] Testing getCart...');
      setError(null);
      // User ID is automatically extracted from JWT token by backend
      const cart = await getCart();
      console.log('[DEBUG] Cart result:', cart);
      setResult(JSON.stringify(cart, null, 2));
    } catch (err) {
      console.error('[DEBUG] Error:', err);
      setError(err.message);
    }
  };

  const testAddToCart = async () => {
    try {
      console.log('[DEBUG] Testing addToCart...');
      setError(null);
      const product = {
        id: 'test-123',
        platform: 'aliexpress',
        title: 'Test Product',
        price: 99.99,
        currency: 'USD',
        image: 'https://via.placeholder.com/100',
        quantity: 1
      };
      // User ID is automatically extracted from JWT token by backend
      const cart = await addToCart(product);
      console.log('[DEBUG] Add result:', cart);
      setResult(JSON.stringify(cart, null, 2));
    } catch (err) {
      console.error('[DEBUG] Error:', err);
      setError(err.message);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-4">Cart Debug Page</h1>

      <div className="space-y-4">
        <button
          onClick={testGetCart}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          Test Get Cart
        </button>

        <button
          onClick={testAddToCart}
          className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 ml-4"
        >
          Test Add to Cart
        </button>
      </div>

      {error && (
        <div className="mt-4 p-4 bg-red-100 text-red-700 rounded">
          <strong>Error:</strong> {error}
        </div>
      )}

      {result && (
        <div className="mt-4">
          <h2 className="font-bold mb-2">Result:</h2>
          <pre className="bg-gray-100 p-4 rounded overflow-auto">
            {result}
          </pre>
        </div>
      )}
    </div>
  );
};

export default CartDebugPage;
