package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.MeetingTemplate;

public interface MeetingTemplateRepository extends JpaRepository<MeetingTemplate, Long> {
    List<MeetingTemplate> findByActiveOrderBySortOrderAscTemplateNameAsc(Boolean active);

    List<MeetingTemplate> findByTemplateCategoryCodeAndActiveOrderBySortOrderAscTemplateNameAsc(String templateCategoryCode, Boolean active);

    List<MeetingTemplate> findByTemplateCategoryCode(String templateCategoryCode);

    List<MeetingTemplate> findAllByOrderBySortOrderAscTemplateNameAsc();

    Optional<MeetingTemplate> findByAnalysisTypeCode(String analysisTypeCode);

    List<MeetingTemplate> findByTemplateCategoryCodeAndAnalysisTypeCode(String templateCategoryCode, String analysisTypeCode);

    Optional<MeetingTemplate> findByAnalysisTypeCodeAndActive(String analysisTypeCode, Boolean active);

    Optional<MeetingTemplate> findByTemplateCategoryCodeAndAnalysisTypeCodeAndActive(
            String templateCategoryCode,
            String analysisTypeCode,
            Boolean active
    );

    Optional<MeetingTemplate> findByTemplateCategoryCodeAndTemplateNameAndActive(
            String templateCategoryCode,
            String templateName,
            Boolean active
    );
}
