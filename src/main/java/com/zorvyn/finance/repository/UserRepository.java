package com.zorvyn.finance.repository;

import com.zorvyn.finance.model.User;
import com.zorvyn.finance.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("""
            SELECT u FROM User u
            WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:active IS NULL OR u.active = :active)
            """)
    Page<User> findWithFilters(
            @Param("username") String username,
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
