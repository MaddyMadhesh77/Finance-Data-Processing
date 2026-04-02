package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.CreateUserRequest;
import com.zorvyn.finance.dto.UpdateUserRequest;
import com.zorvyn.finance.dto.UserResponse;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_encodesPasswordAndReturnsResponse() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("test.user");
        request.setPassword("Password!123");
        request.setRole(Role.ANALYST);

        when(userRepository.existsByUsername("test.user")).thenReturn(false);
        when(passwordEncoder.encode("Password!123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("test.user", userCaptor.getValue().getUsername());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals(Role.ANALYST, userCaptor.getValue().getRole());
        assertEquals(42L, response.getId());
        assertEquals("test.user", response.getUsername());
        assertEquals(Role.ANALYST, response.getRole());
    }

    @Test
    void updateUser_appliesPartialChanges() {
        User existingUser = User.builder()
                .id(7L)
                .username("alice")
                .password("encoded")
                .role(Role.VIEWER)
                .active(true)
                .build();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(Role.ADMIN);
        request.setActive(false);

        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(7L, request);

        assertEquals(Role.ADMIN, existingUser.getRole());
        assertFalse(existingUser.isActive());
        assertEquals(Role.ADMIN, response.getRole());
        assertFalse(response.isActive());
        verify(userRepository).save(existingUser);
    }

    @Test
    void getAllUsers_appliesSearchAndPagination() {
        User user = User.builder()
                .id(9L)
                .username("searchable")
                .password("encoded")
                .role(Role.VIEWER)
                .active(true)
                .build();

        when(userRepository.findWithFilters(eq("search"), eq(Role.VIEWER), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        var response = userService.getAllUsers(" search ", Role.VIEWER, true, 1, 15, "username", "asc");

        assertEquals(1, response.getTotalElements());
        assertEquals("searchable", response.getContent().get(0).getUsername());
        verify(userRepository).findWithFilters(eq("search"), eq(Role.VIEWER), eq(true), any(Pageable.class));
    }

    @Test
    void getAllUsers_invalidSortField_throwsBadRequestSignal() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getAllUsers(null, null, null, 0, 20, "password", "asc")
        );

        assertTrue(ex.getMessage().contains("Invalid sortBy"));
        verify(userRepository, never()).findWithFilters(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void deleteUser_deactivatesAccount() {
        User existingUser = User.builder()
                .id(10L)
                .username("inactive-me")
                .password("encoded")
                .role(Role.ANALYST)
                .active(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.deleteUser(10L);

        assertFalse(existingUser.isActive());
        assertFalse(response.isActive());
        verify(userRepository).save(existingUser);
    }
}