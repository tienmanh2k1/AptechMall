import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingCart } from 'lucide-react';

const CartEmpty = () => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      <div className="bg-gray-100 rounded-full p-8 mb-6">
        <ShoppingCart className="w-16 h-16 text-gray-400" />
      </div>

      <h2 className="text-2xl font-semibold text-gray-900 mb-2">
        Your cart is empty
      </h2>

      <p className="text-gray-600 mb-8 text-center max-w-md">
        Looks like you haven't added any items to your cart yet. Start shopping to fill it up!
      </p>

      <Link
        to="/search"
        className="inline-block px-6 py-3 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
      >
        Start Shopping
      </Link>
    </div>
  );
};

export default CartEmpty;
