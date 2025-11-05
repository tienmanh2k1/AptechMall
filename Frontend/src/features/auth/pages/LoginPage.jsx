import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import { LogIn, Mail, Lock, Eye, EyeOff } from 'lucide-react';
import { jwtDecode } from 'jwt-decode';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../../cart/context/CartContext';
import { login as loginApi, googleOauth, getCurrentUser, generateRefreshOauth } from '../services';
import { GoogleLogin } from '@react-oauth/google';

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const { refreshCart } = useCart();

  // Clear expired tokens on mount
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        const currentTime = Date.now() / 1000;

        if (decoded.exp <= currentTime) {
          // Token expired, clear it
          console.log('🗑️ [LoginPage] Clearing expired token from localStorage');
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        }
      } catch (error) {
        // Invalid token, clear it
        console.log('🗑️ [LoginPage] Clearing invalid token from localStorage');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
  }, []);

  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});

  // Get redirect path from location state or default to home
  const from = location.state?.from?.pathname || '/';

  const validateForm = () => {
    const newErrors = {};

    // Username validation (can be username or email)
    if (!formData.username) {
      newErrors.username = 'Username or email is required';
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleGoogleLogin = async (e) => {

    try {
      setLoading(true);
    const jwtToken = e.credential;
    const base64Payload = jwtToken.split('.')[1];
    const payload = JSON.parse(atob(base64Payload));


    const authRequest = {
      email: payload.email,
      fullName: payload.name,
      googleSub: payload.sub
    };

    console.log("Email from oAuth: ", JSON.stringify(payload, null, 2))
    console.log("Sub: " + authRequest.googleSub + " " + payload.sub);
    const username = formData.username.length === 0 ? authRequest.email.split("@")[0].replace(/[^a-zA-Z0-9_]/g, "_") : formData.username;
    const response = await googleOauth(authRequest, username);

    // if (response.status === 200){
      const token = response.token;
      if (!token) {
        throw new Error('No token received from server');
      }
      const user = {
        username: username,
      };

      generateRefreshOauth();

      login(token, user);

      console.log('🛒 [LoginPage] Refreshing cart after successful login');
      refreshCart();
      setLoading(false);
      toast.success('Login successful!');
      navigate("/", { replace: true });
    // }

    } catch (error){
      setLoading(false);
      console.error('Login error:', error);
      if (error.response.status === 409){
        setErrors("Username from email might have been registered by other users, try filling an alternative username in the form as your secondary");
      }
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);

      // Call login API
      const response = await loginApi({
        username: formData.username, // Backend expects "username" (can be username or email)
        password: formData.password,
      });

      // Backend returns: { token: "eyJhbGci..." }
      const token = response.token;

      if (!token) {
        throw new Error('No token received from server');
      }

      // Fetch full user profile to get role and other details
      let user = {
        username: formData.username,
      };

      try {
        const userProfile = await getCurrentUser();
        user = userProfile;
      } catch (error) {
        console.warn('Could not fetch user profile, using basic info:', error);
        // Continue with basic user info if profile fetch fails
      }

      // Save to context and localStorage
      login(token, user);

      // Refresh cart count after login
      console.log('🛒 [LoginPage] Refreshing cart after successful login');
      refreshCart();

      toast.success('Login successful!');

      // Redirect to the page user came from or home
      navigate(from, { replace: true });
    } catch (error) {
      console.error('Login error:', error);

      let errorMessage = 'Login failed';

      if (error.response) {
        // HTTP error response from server
        const status = error.response.status;
        const data = error.response.data;

        if (status === 401) {
          errorMessage = 'Invalid username or password';
        } else if (status === 404) {
          errorMessage = 'User not found';
        } else if (status === 400) {
          errorMessage = data?.message || 'Invalid request data';
        } else {
          errorMessage = data?.message || data?.error || 'Login failed';
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        {/* Card */}
        <div className="bg-white rounded-2xl shadow-xl p-8">
          {/* Header */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-orange-500 to-red-600 rounded-full mb-4">
              <LogIn className="w-8 h-8 text-white" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">Welcome Back</h2>
            <p className="text-gray-600">Sign in to your PandaMall account</p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Username Field */}
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
                Username or Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="username"
                  name="username"
                  type="text"
                  autoComplete="username"
                  value={formData.username}
                  onChange={handleChange}
                  className={`block w-full pl-10 pr-3 py-3 border ${
                    errors.username ? 'border-red-500' : 'border-gray-300'
                  } rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent`}
                  placeholder="username or email@example.com"
                />
              </div>
              {errors.username && <p className="mt-1 text-sm text-red-600">{errors.username}</p>}
            </div>

            {/* Password Field */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={formData.password}
                  onChange={handleChange}
                  className={`block w-full pl-10 pr-12 py-3 border ${
                    errors.password ? 'border-red-500' : 'border-gray-300'
                  } rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent`}
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5 text-gray-400 hover:text-gray-600" />
                  ) : (
                    <Eye className="h-5 w-5 text-gray-400 hover:text-gray-600" />
                  )}
                </button>
              </div>
              {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password}</p>}
            </div>

            {/* Remember & Forgot Password */}
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  className="h-4 w-4 text-orange-600 focus:ring-orange-500 border-gray-300 rounded"
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700">
                  Remember me
                </label>
              </div>

              <div className="text-sm">
                <Link to="/forgot-password" className="font-medium text-orange-600 hover:text-orange-500">
                  Forgot password?
                </Link>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <LogIn className="w-5 h-5" />
                  <span>Sign In</span>
                </>
              )}
            </button>
          </form>
          <br/>
          <GoogleLogin
            onSuccess={handleGoogleLogin}
            onError={() => console.log('Login Failed')}
          />

          {/* Footer */}
          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white text-gray-500">New to PandaMall?</span>
              </div>
            </div>

            <div className="mt-6">
              <Link
                to="/register"
                className="w-full flex justify-center py-3 px-4 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 transition-colors"
              >
                Create an account
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
