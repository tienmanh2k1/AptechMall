import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/context/AuthContext';
import { getCurrentUser } from '../../auth/services/authApi';
import Loading from '../../../shared/components/Loading';
import { jwtDecode } from 'jwt-decode';

/**
 * AdminRoute Component
 * Wraps routes that require admin or staff authentication
 * Redirects to login if user is not authenticated or not admin/staff
 */
const AdminRoute = ({ children }) => {
  const { isAuthenticated, loading, token, user, updateUser } = useAuth();
  const location = useLocation();
  const [checkingAdmin, setCheckingAdmin] = useState(true);
  const [hasAccess, setHasAccess] = useState(false);

  useEffect(() => {
    const checkAdminAccess = async () => {
      if (loading) return;

      if (!isAuthenticated()) {
        setCheckingAdmin(false);
        return;
      }

      try {
        // Check role from JWT token first
        if (token) {
          try {
            const decoded = jwtDecode(token);
            // JWT token may have role in authorities or role field
            const role = decoded.role ||
                        (decoded.authorities && decoded.authorities[0]?.replace('ROLE_', '')) ||
                        (decoded.authorities && decoded.authorities[0]);

            // Allow both ADMIN and STAFF
            if (role === 'ADMIN' || role === 'ROLE_ADMIN' || role === 'STAFF' || role === 'ROLE_STAFF') {
              setHasAccess(true);
              setCheckingAdmin(false);
              return;
            }
          } catch (error) {
            console.error('Error decoding token:', error);
          }
        }

        // If role not in token, check user object
        if (user && (user.role === 'ADMIN' || user.role === 'STAFF' || user.username === 'admin')) {
          setHasAccess(true);
          setCheckingAdmin(false);
          return;
        }

        // Fetch user profile to get role
        try {
          const userProfile = await getCurrentUser();
          if (userProfile.role === 'ADMIN' || userProfile.role === 'STAFF') {
            updateUser(userProfile);
            setHasAccess(true);
          } else {
            setHasAccess(false);
          }
        } catch (error) {
          console.error('Error fetching user profile:', error);
          setHasAccess(false);
        }
      } catch (error) {
        console.error('Error checking admin access:', error);
        setHasAccess(false);
      } finally {
        setCheckingAdmin(false);
      }
    };

    checkAdminAccess();
  }, [loading, isAuthenticated, token, user, updateUser]);

  // Show loading while checking authentication
  if (loading || checkingAdmin) {
    return <Loading message="Checking access..." />;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Redirect to home if not admin or staff
  if (!hasAccess) {
    return <Navigate to="/" replace />;
  }

  // User has access, render the protected content
  return children;
};

export default AdminRoute;

