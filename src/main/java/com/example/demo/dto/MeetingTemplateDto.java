package com.example.demo.dto;

import com.example.demo.entity.MeetingTemplate;

import lombok.Getter;

@Getter
public class MeetingTemplateDto {
    private final Long id;
    private final String analysisTypeCode;
    private final String templateCategoryCode;
    private final String templateName;
    private final String modelType;
    private final String systemPromptTemplate;
    private final String userPromptTemplate;
    private final Boolean speakerAnalysisEnabled;
    private final Integer sortOrder;
    private final Boolean active;

    public MeetingTemplateDto(MeetingTemplate template) {
        this.id = template.getId();
        this.analysisTypeCode = template.getAnalysisTypeCode();
        this.templateCategoryCode = template.getTemplateCategoryCode();
        this.templateName = template.getTemplateName();
        this.modelType = template.getModelType();
        this.systemPromptTemplate = template.getSystemPromptTemplate();
        this.userPromptTemplate = template.getUserPromptTemplate();
        this.speakerAnalysisEnabled = template.getSpeakerAnalysisEnabled() == null || template.getSpeakerAnalysisEnabled();
        this.sortOrder = template.getSortOrder();
        this.active = template.getActive();
    }
}
