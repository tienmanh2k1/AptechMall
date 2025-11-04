package com.aptech.aptechMall.service.admin;

import com.aptech.aptechMall.dto.user.UserResponseDTO;
import com.aptech.aptechMall.dto.user.UserUpdateDTO;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
