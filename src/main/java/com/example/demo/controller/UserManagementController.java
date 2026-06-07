package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserApprovalRequestDto;
import com.example.demo.dto.UserManagementDto;
import com.example.demo.dto.UserTemplatePermissionSaveRequestDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthService authService;

    @GetMapping
    public List<UserManagementDto> users(
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return userManagementService.getUsers();
    }

    @PostMapping("/approve")
    public UserManagementDto approve(
            @RequestBody UserApprovalRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return userManagementService.approve(dto);
    }

    @PostMapping("/template-permissions")
    public String saveTemplatePermissions(
            @RequestBody UserTemplatePermissionSaveRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);
        userManagementService.saveTemplatePermissions(dto);

        return "저장완료";
    }

    @DeleteMapping("/{userId}")
    public String deleteUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);
        userManagementService.deleteUser(userId, loginEmail);

        return "삭제완료";
    }
}
