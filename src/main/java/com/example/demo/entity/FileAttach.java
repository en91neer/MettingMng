package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_attach")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 업무구분
     * 예)
     * MEETING_MINUTES
     * MEETING
     * NOTICE
     */
    @Column(name = "target_type_code", length = 50)
    private String targetTypeCode;

    /**
     * 업무 PK
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * 원본 파일명
     */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /**
     * 저장 파일명(uuid)
     */
    @Column(name = "saved_name", nullable = false)
    private String savedName;

    /**
     * 음성파일 경로
     */
    @Column(name = "audio_file_path", length = 1000)
    private String audioFilePath;

    /**
     * Whisper 원본 텍스트 파일 경로
     */
    @Column(name = "transcript_file_path", length = 1000)
    private String transcriptFilePath;

    /**
     * GPT 요약본 파일 경로
     */
    @Column(name = "summary_file_path", length = 1000)
    private String summaryFilePath;

    /**
     * AI 분석 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "analyze_status", length = 20)
    private AnalyzeStatus analyzeStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.analyzeStatus == null) {
            this.analyzeStatus = AnalyzeStatus.WAITING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
