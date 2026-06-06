package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthVerifyRequestDto {
    private String email;
    private String code;
    private boolean rememberOneDay;
}
