package com.example.Application.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponseDTO {
    private String token;
    private String username;
    private String fullName;
    private String role;
    private Long userId;
}
