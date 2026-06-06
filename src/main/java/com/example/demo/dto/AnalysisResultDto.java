package com.example.demo.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.example.demo.entity.AnalysisResult;

import lombok.Getter;

@Getter
public class AnalysisResultDto {
    private final Long id;
    private final Long fileId;
    private final String analysisType;
    private final String analysisTypeCode;
    private final String content;
    private final String analyzedAt;

    public AnalysisResultDto(AnalysisResult analysisResult) {
        this.id = analysisResult.getId();
        this.fileId = analysisResult.getFileId();
        this.analysisType = analysisResult.getAnalysisTypeCode();
        this.analysisTypeCode = analysisResult.getAnalysisTypeCode();
        this.content = analysisResult.getContent();
        this.analyzedAt = formatDateTime(analysisResult.getAnalyzedAt());
    }

    public AnalysisResultDto(
            Long id,
            Long fileId,
            String analysisType,
            String analysisTypeCode,
            String content,
            LocalDateTime analyzedAt
    ) {
        this.id = id;
        this.fileId = fileId;
        this.analysisType = analysisType;
        this.analysisTypeCode = analysisTypeCode;
        this.content = content;
        this.analyzedAt = formatDateTime(analyzedAt);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
