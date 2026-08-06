package com.vanshika.api_rate_limiter_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration for the API Rate Limiter Service.
 *
 * This class enforces role-based access control (RBAC) on sensitive endpoints.
 * Only users with the ADMIN role can perform tier management operations.
 *
 * For this demo, credentials are stored in-memory.
 * In production, this would be replaced with a JWT-based authentication
 * system backed by a database or an external Identity Provider (e.g., Keycloak, AWS Cognito).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security rules for all HTTP endpoints.
     *
     * Rules (evaluated top to bottom — first match wins):
     * 1. PATCH /api/users/{any}/tier → requires ADMIN role
     * 2. All other requests          → permitted without authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST APIs — REST clients are stateless, no session cookies
            .csrf(csrf -> csrf.disable())

            // Define endpoint-level access rules
            .authorizeHttpRequests(auth -> auth

                // ADMIN-ONLY: Only the tier upgrade endpoint requires authentication
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/tier").hasRole("ADMIN")

                // Everything else is open — Employee APIs, User creation, etc.
                .anyRequest().permitAll()
            )

            // Use HTTP Basic Authentication for simplicity in this demo
            // In production: replace with .oauth2ResourceServer() or JWT filter
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Defines the in-memory user store with one admin user.
     *
     * Credentials for demo:
     *   Username: admin
     *   Password: admin123
     *   Role:     ADMIN
     *
     * In production: UserDetailsService would load users from a database
     * or delegate to an external Identity Provider via OAuth2/OIDC.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * BCrypt password encoder — industry standard for secure password hashing.
     * BCrypt automatically handles salting, making rainbow table attacks impossible.
     * In production, the same encoder would be used when storing user passwords in the DB.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
