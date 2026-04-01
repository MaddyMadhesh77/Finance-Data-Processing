package com.zorvyn.finance.config;

import com.zorvyn.finance.security.CustomUserDetailsService;
import com.zorvyn.finance.security.JwtAuthenticationFilter;
import com.zorvyn.finance.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * Authentication strategy: JWT bearer tokens for API access.
 * Session strategy: Stateless (no session cookie maintained).
 *
 * URL-level access rules:
 *   /h2-console/**        -> permitAll (development tool)
 *   POST /api/users       -> ADMIN only (create user)
 *   PATCH/DELETE /api/users/** -> ADMIN only
 *   GET /api/users/**     -> ADMIN only
 *   POST/PUT/DELETE /api/records/** -> ADMIN only
 *   GET /api/records/**   -> ADMIN and ANALYST
 *   GET /api/dashboard/** -> ADMIN and ANALYST
 *   GET /api/dashboard/summary -> also VIEWER (read-only overview)
 *
 * Fine-grained method-level security is also enabled via @PreAuthorize for
 * scenarios that require more complex per-principal checks.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — APIs are stateless and consumed by non-browser clients
            .csrf(AbstractHttpConfigurer::disable)
            // Allow H2 console frames in the browser
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // H2 console — dev tool only
                .requestMatchers("/h2-console/**").permitAll()

                // OpenAPI / Swagger UI — public in development and demo environments
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Authentication endpoint
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                // User management — ADMIN only
                .requestMatchers(HttpMethod.GET,    "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // Financial record mutations — ADMIN only
                .requestMatchers(HttpMethod.POST,   "/api/records").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/records/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/records/**").hasRole("ADMIN")

                // Financial record reads — ADMIN and ANALYST
                .requestMatchers(HttpMethod.GET,    "/api/records/**").hasAnyRole("ADMIN", "ANALYST")

                // Dashboard summaries — ADMIN, ANALYST, and VIEWER
                .requestMatchers(HttpMethod.GET,    "/api/dashboard/**").hasAnyRole("ADMIN", "ANALYST", "VIEWER")

                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
