package com.example.Application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/login", "/register", "/visitor/register", "/visitor/verify-otp").permitAll()
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                // Public APIs
                .requestMatchers("/api/auth/**", "/api/visitor/register", "/api/visitor/verify-otp").permitAll()
                // Gate APIs (guard access)
                .requestMatchers("/api/gate/**", "/gate/**").permitAll()
                // Emergency APIs (broadcast, no auth needed for alerts)
                .requestMatchers("/api/emergency/**", "/emergency/**").permitAll()
                // Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Actuator health
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // H2 console
                .requestMatchers("/h2-console/**").permitAll()
                // WebSocket
                .requestMatchers("/ws/**").permitAll()
                // Role-specific dashboard pages
                .requestMatchers("/founder-dashboard").hasAnyAuthority("ROLE_FOUNDER")
                .requestMatchers("/manager-dashboard").hasAnyAuthority("ROLE_FOUNDER","ROLE_MANAGER")
                .requestMatchers("/admin").hasAnyAuthority("ROLE_FOUNDER","ROLE_MANAGER","ROLE_ADMIN")
                .requestMatchers("/front-office").hasAnyAuthority("ROLE_FOUNDER","ROLE_MANAGER","ROLE_ADMIN","ROLE_FRONT_OFFICE")
                .requestMatchers("/room-service").hasAnyAuthority("ROLE_FOUNDER","ROLE_MANAGER","ROLE_ADMIN","ROLE_ROOM_SERVICE")
                .requestMatchers("/chef-dashboard").hasAnyAuthority("ROLE_FOUNDER","ROLE_MANAGER","ROLE_ADMIN","ROLE_CHEF")
                .requestMatchers("/client-portal").hasAnyAuthority("ROLE_CLIENT","ROLE_FOUNDER","ROLE_MANAGER","ROLE_ADMIN")
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
