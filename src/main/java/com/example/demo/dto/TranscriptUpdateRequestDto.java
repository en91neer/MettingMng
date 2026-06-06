package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptUpdateRequestDto {
    private Long fileId;
    private String content;
    private String loginEmail;
}
