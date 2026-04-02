package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.*;
import com.zorvyn.finance.exception.ConflictException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Handles all User management logic: creation, retrieval, role assignment,
 * status toggling (active/inactive), and filtered listing.
 *
 * Assumptions:
 *  - Only Admins reach this service (enforced at SecurityConfig URL level).
 *  - Username uniqueness is validated here before persistence.
 *  - Deleting a user is soft-delete only — deactivation is used instead
 *    to preserve audit integrity.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "username", "role", "active");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Create a new user. Username must be unique. */
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    /** Return users with optional search filters and pagination. */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(
            String username,
            Role role,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        return userRepository.findWithFilters(normalize(username), role, active, pageable)
                .map(this::toResponse);
    }

    /** Soft-delete a user by deactivating the account. */
    public UserResponse deleteUser(Long id) {
        User user = findUserOrThrow(id);
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    /**
     * Partially update a user: role and/or active status.
     * Supports null fields — only non-null fields are applied.
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return toResponse(userRepository.save(user));
    }

    /** Return a single user by ID. */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    /** Map entity → safe response DTO (no password). */
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = normalize(sortBy);
        if (field == null) {
            field = "createdAt";
        }
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Invalid sortBy. Allowed values: createdAt, username, role, active");
        }

        String direction = normalize(sortDir);
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
