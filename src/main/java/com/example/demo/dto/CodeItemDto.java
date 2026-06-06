package com.example.demo.dto;

import com.example.demo.entity.CodeItem;

import lombok.Getter;

@Getter
public class CodeItemDto {
    private final Long id;
    private final String groupCode;
    private final String code;
    private final String codeName;
    private final String description;
    private final Integer sortOrder;
    private final Boolean active;

    public CodeItemDto(CodeItem item) {
        this.id = item.getId();
        this.groupCode = item.getGroupCode();
        this.code = item.getCode();
        this.codeName = item.getCodeName();
        this.description = item.getDescription();
        this.sortOrder = item.getSortOrder();
        this.active = item.getActive();
    }
}
