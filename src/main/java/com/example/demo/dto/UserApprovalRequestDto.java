package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserApprovalRequestDto {
    private Long userId;
    private String roleCode;
}
