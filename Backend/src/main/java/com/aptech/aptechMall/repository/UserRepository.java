package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     * @param email User email
     * @return Optional containing User if found
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :username")
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email
     * @param email User email
     * @return true if exists
     */
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Count users by status
     * @param status User status (ACTIVE, SUSPENDED, DELETED)
     * @return Number of users with the given status
     */
    long countByStatus(Status status);

    /**
     * Count users by role
     * @param role User role (ADMIN, STAFF, CUSTOMER)
     * @return Number of users with the given role
     */
    long countByRole(Role role);
}
