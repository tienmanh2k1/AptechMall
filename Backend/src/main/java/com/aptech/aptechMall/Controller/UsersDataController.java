package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.Exception.UserNotFoundException;
import com.aptech.aptechMall.dto.user.UserResponseDTO;
import com.aptech.aptechMall.dto.user.UserUpdateDTO;
import com.aptech.aptechMall.security.requests.RegisterRequest;
import com.aptech.aptechMall.security.requests.RegisterResponse;
import com.aptech.aptechMall.service.admin.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/api/users")
@Slf4j
public class UsersDataController {

    private final UserManagementService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<RegisterResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(service.create(request));
        } catch (RuntimeException e) {
            log.error("Problem with create operation: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(@RequestParam(name = "page", defaultValue = "0") int page,
                                                             @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponseDTO> users = service.getAllUsers(pageable);
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            log.error("Problem with fetch all operation: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        try {
            return service.getUserById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e){
            log.error("Problem with fetch operation: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        try {
            UserResponseDTO updatedUser = service.updateUser(id, dto);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("Problem with update operation: "+ e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            UserResponseDTO updatedUser = service.patchUser(id, updates);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e){
            log.error("Problem with patch operation: "+ e.getMessage());
            return ResponseEntity.internalServerError().body("Technical issue");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            service.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Problem with deletion operation: "+ e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
