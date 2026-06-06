package com.example.demo.dto;

import java.util.List;

import com.example.demo.entity.User;

import lombok.Getter;

@Getter
public class UserManagementDto {
    private final Long id;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final String roleCode;
    private final String statusCode;
    private final List<String> allowedTemplateCodes;

    public UserManagementDto(User user) {
        this(user, List.of());
    }

    public UserManagementDto(User user, List<String> allowedTemplateCodes) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.roleCode = user.getRoleCode();
        this.statusCode = user.getStatusCode();
        this.allowedTemplateCodes = allowedTemplateCodes;
    }
}
