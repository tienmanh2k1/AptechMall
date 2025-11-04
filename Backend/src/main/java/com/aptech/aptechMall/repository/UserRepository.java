package com.aptech.aptechMall.repository;


import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    /**
     * Find user by email
     * @param email User email
     * @return Optional containing User if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email
     * @param email User email
     * @return true if exists
     */
    boolean existsByEmail(String email);

    @Query(value = """
        SELECT * FROM users
        WHERE JSON_UNQUOTE(JSON_EXTRACT(o_auth, '$.email')) = :email
          AND JSON_UNQUOTE(JSON_EXTRACT(o_auth, '$.sub')) = :sub
          AND JSON_EXTRACT(o_auth, '$.verified') = true
        """, nativeQuery = true)
    Optional<User> findByOAuthEmail(
            @Param("email") String email,
            @Param("sub") String sub
    );

    int countByRole(Role role);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.email = :email")
    Long getOrderCount(@Param("email") String email);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.email = :email AND o.status = 'DELIVERED'")
    BigDecimal getTotalSpent(@Param("email") String email);

}
