package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeGroupSaveRequestDto {
    private Long id;
    private String groupCode;
    private String groupName;
    private String description;
    private Boolean active;
}
