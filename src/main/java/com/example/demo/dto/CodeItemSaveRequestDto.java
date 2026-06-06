package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeItemSaveRequestDto {
    private Long id;
    private String groupCode;
    private String code;
    private String codeName;
    private String description;
    private Integer sortOrder;
    private Boolean active;
}
