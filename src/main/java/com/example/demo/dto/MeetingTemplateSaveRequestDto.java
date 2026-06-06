package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingTemplateSaveRequestDto {
    private Long id;
    private String analysisTypeCode;
    private String templateCategoryCode;
    private String templateName;
    private String modelType;
    private String systemPromptTemplate;
    private String userPromptTemplate;
    private Boolean speakerAnalysisEnabled;
    private Integer sortOrder;
    private Boolean active;
}
