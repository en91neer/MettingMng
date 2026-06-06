package com.example.demo.dto;

import com.example.demo.entity.CodeGroup;

import lombok.Getter;

@Getter
public class CodeGroupDto {
    private final Long id;
    private final String groupCode;
    private final String groupName;
    private final String description;
    private final Boolean active;

    public CodeGroupDto(CodeGroup group) {
        this.id = group.getId();
        this.groupCode = group.getGroupCode();
        this.groupName = group.getGroupName();
        this.description = group.getDescription();
        this.active = group.getActive();
    }
}
