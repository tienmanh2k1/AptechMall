package com.aptech.aptechMall.security;

import com.aptech.aptechMall.model.jpa.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for extracting authenticated user information from SecurityContext
 * Provides convenient methods to get current user details in controllers and services
 */
@Component
public class AuthenticationUtil {

    /**
     * Get the currently authenticated User object from SecurityContext
     *
     * @return User object of the authenticated user
     * @throws IllegalStateException if no user is authenticated or principal is not a User instance
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User)) {
            throw new IllegalStateException("Principal is not a User instance");
        }

        return (User) principal;
    }

    /**
     * Get the userId of the currently authenticated user
     *
     * @return userId of the authenticated user
     * @throws IllegalStateException if no user is authenticated or userId is null
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        Long userId = user.getUserId();

        if (userId == null) {
            throw new IllegalStateException(
                "User ID not found in authentication token. " +
                "This may be due to an outdated token. Please logout and login again to get a new token."
            );
        }

        return userId;
    }

    /**
     * Get the username of the currently authenticated user
     *
     * @return username of the authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    /**
     * Get the email of the currently authenticated user
     *
     * @return email of the authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    /**
     * Get the role of the currently authenticated user
     *
     * @return Role of the authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public static Role getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /**
     * Check if a user is currently authenticated
     *
     * @return true if a user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               authentication.getPrincipal() instanceof User;
    }
}
