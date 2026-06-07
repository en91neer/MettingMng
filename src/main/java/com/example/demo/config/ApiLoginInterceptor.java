package com.example.demo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiLoginInterceptor implements HandlerInterceptor {

    private static final String LOGIN_REQUIRED_MESSAGE = "로그인 후 사용해주세요.";

    private final AuthService authService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (
                "POST".equalsIgnoreCase(request.getMethod())
                        && request.getRequestURI().endsWith("/api/implementation-inquiries")
        ) {
            return true;
        }

        try {
            authService.validateSession(
                    request.getHeader("X-Login-Email"),
                    request.getHeader("X-Auth-Token")
            );
            return true;
        } catch (RuntimeException error) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"message\":\"" + LOGIN_REQUIRED_MESSAGE + "\"}"
            );
            return false;
        }
    }
}
