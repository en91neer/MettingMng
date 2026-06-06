package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalysisResultUpdateRequestDto {
    private Long fileId;
    private String analysisType;
    private String analysisTypeCode;
    private String content;
    private String loginEmail;
}
