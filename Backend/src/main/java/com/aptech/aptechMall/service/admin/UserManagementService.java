package com.aptech.aptechMall.service.admin;

import com.aptech.aptechMall.dto.user.UserResponseDTO;
import com.aptech.aptechMall.dto.user.UserUpdateDTO;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service quản lý user (Admin operation)
 *
 * Chức năng chính:
 * - Xem danh sách tất cả users
 * - Xem chi tiết user theo ID
 * - Cập nhật thông tin user (full update hoặc patch)
 * - Xóa user
 *
 * BẢO MẬT QUAN TRỌNG:
 *
 * 1. **Admin không thể thay đổi role của chính mình**:
 *    - Prevent admin tự demote hoặc promote chính mình
 *    - Đảm bảo admin phải có người khác thay đổi role
 *    - Log warning nếu có attempt
 *
 * 2. **Admin không thể xóa chính mình**:
 *    - Prevent admin tự xóa account đang đăng nhập
 *    - Đảm bảo admin account không mất quyền truy cập
 *
 * 3. **Không được demote/delete admin cuối cùng**:
 *    - Check số lượng admin còn lại (countByRole)
 *    - Nếu chỉ còn 1 admin → không cho phép demote hoặc delete
 *    - Đảm bảo luôn có ít nhất 1 admin trong hệ thống
 *
 * ROLES:
 * - **ADMIN**: Full access, quản lý tất cả
 * - **STAFF**: Quản lý orders, wallets (không quản lý users)
 * - **CUSTOMER**: User thông thường, chỉ xem/sửa profile của mình
 *
 * STATUS:
 * - **ACTIVE**: Tài khoản hoạt động bình thường
 * - **INACTIVE**: Tài khoản bị vô hiệu hóa (không login được)
 * - **BANNED**: Tài khoản bị cấm (vi phạm policy)
 *
 * OPERATIONS:
 *
 * 1. **getAllUsers()**:
 *    - Lấy tất cả users trong hệ thống
 *    - Return DTO (không bao gồm password)
 *    - Admin dashboard hiển thị danh sách
 *
 * 2. **getUserById(Long id)**:
 *    - Lấy chi tiết 1 user theo ID
 *    - Return Optional<UserResponseDTO>
 *
 * 3. **updateUser(Long id, UserUpdateDTO)**:
 *    - Full update: Cập nhật tất cả fields trong DTO
 *    - Security checks: Prevent self role change, prevent last admin demotion
 *
 * 4. **patchUser(Long id, Map<String, Object>)**:
 *    - Partial update: Chỉ update các fields có trong map
 *    - Flexible hơn updateUser (không cần gửi tất cả fields)
 *    - Security checks tương tự
 *
 * 5. **deleteUser(Long id)**:
 *    - Xóa user khỏi hệ thống
 *    - Security checks: Prevent self delete, prevent last admin deletion
 *    - Cascade delete: Orders, Cart, Wallet, v.v. (phụ thuộc entity relationships)
 *
 * USER RESPONSE DTO:
 * - userId, username, email, fullName, avatarUrl
 * - emailVerified, phone
 * - role, status
 * - registeredAt, updatedAt, lastLogin
 * - **KHÔNG bao gồm password** (security)
 *
 * AUTHORIZATION:
 * - Controller phải có @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
 * - Hoặc @Secured({"ROLE_ADMIN", "ROLE_STAFF"})
 * - SecurityConfig đã cấu hình: /api/users/** requires ADMIN or STAFF
 *
 * USE CASES:
 * - Admin dashboard: Xem danh sách users, search, filter
 * - Admin edit user: Thay đổi role (CUSTOMER → STAFF), thay đổi status
 * - Admin ban user: Set status = BANNED hoặc INACTIVE
 * - Admin delete spam account: Delete user không cần thiết
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    private UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setRegisteredAt(user.getRegisteredAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toDTO);
    }

    @Transactional
    public UserResponseDTO patchUser(Long id, Map<String, Object> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent admin from changing their own role
        Long currentUserId = AuthenticationUtil.getCurrentUserId();
        if (id.equals(currentUserId) && updates.containsKey("role")) {
            throw new RuntimeException("You cannot change your own role");
        }

        // Prevent last admin demotion
        if (updates.containsKey("role")) {
            String newRoleStr = (String) updates.get("role");
            Role newRole = Role.valueOf(newRoleStr);

            if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
                long adminCount = userRepository.countByRole(Role.ADMIN);
                if (adminCount <= 1) {
                    throw new RuntimeException("Cannot demote the last admin account");
                }
            }
        }

        updates.forEach((key, value) -> {
            switch (key) {
                case "email" -> user.setEmail((String) value);
                case "fullName" -> user.setFullName((String) value);
                case "role" -> user.setRole(Role.valueOf((String) value));
                case "status" -> user.setStatus(Status.valueOf((String) value));
                case "phone" -> user.setPhone((String) value);
            }
        });

        userRepository.save(user);
        return toDTO(user);
    }

    public UserResponseDTO updateUser(Long id, UserUpdateDTO dto) {
        // Prevent admin from changing their own role
        Long currentUserId = AuthenticationUtil.getCurrentUserId();
        if (id.equals(currentUserId) && dto.getRole() != null) {
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            if (!dto.getRole().equals(currentUser.getRole())) {
                throw new RuntimeException("You cannot change your own role");
            }
        }

        // Prevent last admin demotion
        if (dto.getRole() != null) {
            User userToUpdate = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
            if (userToUpdate.getRole() == Role.ADMIN && dto.getRole() != Role.ADMIN) {
                long adminCount = userRepository.countByRole(Role.ADMIN);
                if (adminCount <= 1) {
                    throw new RuntimeException("Cannot demote the last admin account");
                }
            }
        }

        return userRepository.findById(id)
                .map(existingUser -> {
                    if (dto.getFullName() != null) existingUser.setFullName(dto.getFullName());
                    if (dto.getEmail() != null) existingUser.setEmail(dto.getEmail());
                    if (dto.getAvatarUrl() != null) existingUser.setAvatarUrl(dto.getAvatarUrl());
                    if (dto.getPhone() != null) existingUser.setPhone(dto.getPhone());
                    if (dto.getRole() != null) existingUser.setRole(dto.getRole());
                    if (dto.getStatus() != null) existingUser.setStatus(dto.getStatus());
                    User updated = userRepository.save(existingUser);
                    return toDTO(updated);
                })
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        // Check if user exists
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }

        // Prevent admin from deleting their own account
        Long currentUserId = AuthenticationUtil.getCurrentUserId();
        if (id.equals(currentUserId)) {
            throw new RuntimeException("You cannot delete your own account");
        }

        // Prevent deletion of the last admin account
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (userToDelete.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot delete the last admin account");
            }
        }

        userRepository.deleteById(id);
    }
}
