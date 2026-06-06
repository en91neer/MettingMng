package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthVerifyResponseDto {
    private String email;
    private String token;
    private LocalDateTime expiresAt;
    private String roleCode;
    private String statusCode;
}
