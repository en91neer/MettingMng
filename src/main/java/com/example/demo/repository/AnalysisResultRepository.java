package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.AnalysisResult;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByFileIdOrderByAnalyzedAtAsc(Long fileId);

    List<AnalysisResult> findByFileIdInOrderByFileIdAscAnalyzedAtAsc(List<Long> fileIds);

    Optional<AnalysisResult> findByFileIdAndAnalysisTypeCode(Long fileId, String analysisTypeCode);

    void deleteByMeetingMinutesId(Long meetingMinutesId);
}
