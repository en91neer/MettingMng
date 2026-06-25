package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthCodeRequestDto;
import com.example.demo.dto.AuthCodeResponseDto;
import com.example.demo.dto.AuthVerifyRequestDto;
import com.example.demo.dto.AuthVerifyResponseDto;
import com.example.demo.dto.SignupRequestDto;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/request-code")
    public AuthCodeResponseDto requestCode(
            @RequestBody AuthCodeRequestDto dto,
            HttpServletRequest request
    ) {
        return authService.requestCode(
                dto.getEmail(),
                getClientIp(request)
        );
    }

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequestDto dto) {
        authService.signup(dto);

        return "회원가입이 완료되었습니다. 이메일 인증 후 로그인할 수 있습니다.22";
    }

    @PostMapping("/verify-code")
    public AuthVerifyResponseDto verifyCode(
            @RequestBody AuthVerifyRequestDto dto,
            HttpServletRequest request
    ) {
        return authService.verifyCode(
                dto.getEmail(),
                dto.getCode(),
                dto.isRememberOneDay(),
                getClientIp(request)
        );
    }

    @PostMapping("/logout")
    public String logout(
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.logout(loginEmail, authToken);

        return "로그아웃 완료";
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
