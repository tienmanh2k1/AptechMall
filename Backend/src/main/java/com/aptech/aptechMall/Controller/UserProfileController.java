package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.ApiResponse;
import com.aptech.aptechMall.dto.user.ChangeEmailRequest;
import com.aptech.aptechMall.dto.user.ChangePasswordRequest;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user profile operations
 * Base path: /api/users/me
 *
 * SECURITY: All endpoints use authenticated user's ID from JWT token.
 * Users can only modify their own profile.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"})
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Change current user's password
     * POST /api/users/me/change-password
     *
     * @param request ChangePasswordRequest with current and new password
     * @return Success message
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("POST /api/users/me/change-password - userId: {}", userId);

        try {
            userProfileService.changePassword(userId, request);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Password changed successfully")
            );
        } catch (BadCredentialsException e) {
            log.warn("Password change failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Password change failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error changing password for user {}", userId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to change password. Please try again.")
            );
        }
    }

    /**
     * Change current user's email
     * POST /api/users/me/change-email
     *
     * @param request ChangeEmailRequest with current password and new email
     * @return Success message
     */
    @PostMapping("/change-email")
    public ResponseEntity<ApiResponse<String>> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request) {

        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("POST /api/users/me/change-email - userId: {}", userId);

        try {
            userProfileService.changeEmail(userId, request);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email changed successfully")
            );
        } catch (BadCredentialsException e) {
            log.warn("Email change failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Email change failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error changing email for user {}", userId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to change email. Please try again.")
            );
        }
    }
}
