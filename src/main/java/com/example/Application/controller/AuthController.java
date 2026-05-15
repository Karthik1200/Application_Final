package com.example.Application.controller;

import com.example.Application.config.JwtUtil;
import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.dto.LoginRequestDTO;
import com.example.Application.dto.LoginResponseDTO;
import com.example.Application.entity.AppUser;
import com.example.Application.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request, HttpServletResponse response) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Invalid username or password"));
        }

        if (!user.isActive()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Account is deactivated"));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());

        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);

        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .userId(user.getId())
                .build();

        return ResponseEntity.ok(ApiResponseDTO.success("Login successful", loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(ApiResponseDTO.success("Logged out successfully", null));
    }
}
