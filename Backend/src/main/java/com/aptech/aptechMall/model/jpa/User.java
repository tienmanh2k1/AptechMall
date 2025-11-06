package com.aptech.aptechMall.model.jpa;

import com.aptech.aptechMall.model.converters.OAuthConverter;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

/**
 * User entity representing registered users in the system
 * Supports authentication and order management
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long Id;

    @Column(unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="last_login")
    private LocalDateTime lastLogin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = OAuthConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> oAuth = new HashMap<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserAddresses> userAddresses = new HashSet<>();

    @Column(nullable = false, columnDefinition = "ENUM('ADMIN', 'STAFF', 'CUSTOMER') DEFAULT 'CUSTOMER'")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, columnDefinition = "ENUM('ACTIVE', 'SUSPENDED', 'DELETED') DEFAULT 'ACTIVE'")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailVerified;

    @Override
    public String getUsername() {
        // Return email if username is null (for email-only users)
        if (username != null) {
            return username;
        }
        if (email != null) {
            return email;
        }
        // Should never happen due to database constraints, but handle safely
        throw new IllegalStateException("User must have either username or email");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return !(status == Status.DELETED);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !(status == Status.SUSPENDED);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == Status.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = Status.ACTIVE;
        }
        if (this.role == null) {
            this.role = Role.CUSTOMER;
        }
    }
}
