package com.example.demo.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserTemplatePermissionSaveRequestDto {
    private String userEmail;
    private List<String> analysisTypeCodes;
}
