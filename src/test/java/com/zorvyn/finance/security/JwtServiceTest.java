package com.zorvyn.finance.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void tokenRoundTrip_extractsUsernameAndValidates() {
        JwtService jwtService = new JwtService("0123456789abcdef0123456789abcdef0123456789abcdef", 60);
        User userDetails = new User("alice", "password", java.util.List.of());

        String token = jwtService.generateToken(userDetails.getUsername());

        assertEquals("alice", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails.getUsername()));
    }
}