package com.aptech.aptechMall.service.admin;

import com.aptech.aptechMall.Exception.UsernameAlreadyTaken;
import com.aptech.aptechMall.dto.user.UserResponseDTO;
import com.aptech.aptechMall.dto.user.UserUpdateDTO;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import com.aptech.aptechMall.security.requests.RegisterRequest;
import com.aptech.aptechMall.security.requests.RegisterResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toDTO);
    }

    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toDTO);
    }

    @Transactional
    public UserResponseDTO patchUser(Long id, Map<String, Object> updates) {
        User currentUser = AuthenticationUtil.getCurrentUser();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        updates.forEach((key, value) -> {
            switch (key) {
                case "email" -> user.setEmail((String) value);
                case "fullName" -> user.setFullName((String) value);
                case "role" -> user.setRole(Role.valueOf((String) value));
                case "status" -> user.setStatus(Status.valueOf((String) value));
                case "phone" -> user.setPhone((String) value);
            }
        });

        if (currentUser.getEmail().equals(user.getEmail()) && !currentUser.getStatus().equals(user.getStatus())) {
            throw new IllegalStateException("You cannot ban/restore your own account");
        } else {
            userRepository.save(user);
            return toDTO(user);
        }
    }

    public UserResponseDTO updateUser(Long id, UserUpdateDTO dto) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    if (dto.getFullName() != null) existingUser.setFullName(dto.getFullName());
                    if (dto.getEmail() != null) existingUser.setEmail(dto.getEmail());
                    if (dto.getAvatarUrl() != null) existingUser.setAvatarUrl(dto.getAvatarUrl());
                    if (dto.getPhone() != null) existingUser.setPhone(dto.getPhone());
                    if (dto.getRole() != null) existingUser.setRole(dto.getRole());
                    User updated = userRepository.save(existingUser);
                    return toDTO(updated);
                })
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("You cannot delete your own account");
        }

        user.setStatus(Status.DELETED);
        userRepository.deleteById(id);
    }

    public RegisterResponse create(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
        }

        Role role = Role.fromString(request.getRole());

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .email(request.getEmail())
                .build();

        userRepository.save(user);

        return new RegisterResponse("Successfully registered the user " + user.getUsername());
    }
}
