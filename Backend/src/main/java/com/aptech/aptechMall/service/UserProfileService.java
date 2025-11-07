package com.aptech.aptechMall.service;

import com.aptech.aptechMall.dto.user.ChangeEmailRequest;
import com.aptech.aptechMall.dto.user.ChangePasswordRequest;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for user profile operations
 * Handles password change, email change, and other profile updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Change user's password
     * @param userId Current user ID (from JWT token)
     * @param request ChangePasswordRequest with current and new password
     * @throws RuntimeException if user not found
     * @throws BadCredentialsException if current password is incorrect
     * @throws IllegalArgumentException if new password is same as current password
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Attempting to change password for user {}", userId);

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Failed to change password for user {}: incorrect current password", userId);
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Validate new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Failed to change password for user {}: new password same as current", userId);
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user {}", userId);
    }

    /**
     * Change user's email
     * @param userId Current user ID (from JWT token)
     * @param request ChangeEmailRequest with current password and new email
     * @throws RuntimeException if user not found
     * @throws BadCredentialsException if current password is incorrect
     * @throws IllegalArgumentException if new email is same as current or already exists
     */
    @Transactional
    public void changeEmail(Long userId, ChangeEmailRequest request) {
        log.info("Attempting to change email for user {}", userId);

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Failed to change email for user {}: incorrect current password", userId);
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Validate new email is different from current
        if (request.getNewEmail().equalsIgnoreCase(user.getEmail())) {
            log.warn("Failed to change email for user {}: new email same as current", userId);
            throw new IllegalArgumentException("New email must be different from current email");
        }

        // Check if new email already exists
        if (userRepository.existsByEmail(request.getNewEmail())) {
            log.warn("Failed to change email for user {}: email {} already in use",
                    userId, request.getNewEmail());
            throw new IllegalArgumentException("Email already in use");
        }

        // Update email
        String oldEmail = user.getEmail();
        user.setEmail(request.getNewEmail());
        userRepository.save(user);

        log.info("Email changed successfully for user {}: {} -> {}",
                userId, oldEmail, request.getNewEmail());

        // TODO: If user logs in with email, regenerate JWT tokens with new email
        // TODO: Send confirmation email to new address
    }
}
