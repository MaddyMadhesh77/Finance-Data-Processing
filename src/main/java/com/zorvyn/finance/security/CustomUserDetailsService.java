package com.zorvyn.finance.security;

import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.DisabledException;

import java.util.List;

/**
 * Bridges the application's User entity with Spring Security's authentication.
 *
 * Inactive users are rejected with a DisabledException at login time,
 * providing clear feedback rather than a generic auth error.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isActive()) {
            throw new DisabledException("User account is disabled: " + username);
        }

        // Spring Security role convention: prefix with "ROLE_"
        String authority = "ROLE_" + user.getRole().name();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
