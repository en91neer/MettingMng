package com.example.demo.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.MeetingTemplateDto;
import com.example.demo.dto.MeetingTemplateSaveRequestDto;
import com.example.demo.entity.CodeItem;
import com.example.demo.entity.MeetingTemplate;
import com.example.demo.entity.UserTemplatePermission;
import com.example.demo.repository.CodeItemRepository;
import com.example.demo.repository.MeetingTemplateRepository;
import com.example.demo.repository.UserTemplatePermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingTemplateService {

    private static final String CATEGORY_ANALYSIS_SUMMARY = "ANALYSIS_SUMMARY";
    private static final String CATEGORY_SPEAKER_SEPARATION = "SPEAKER_SEPARATION";
    private static final String ANALYSIS_TYPE_GENERAL_MEETING = "GENERAL_MEETING";
    private static final String ANALYSIS_TYPE_DEV_MEETING = "DEV_MEETING";
    private static final String ANALYSIS_TYPE_CONSULTING_MEETING = "CONSULTING_MEETING";
    private static final String ANALYSIS_TYPE_SPEAKER_SEPARATION = "SPEAKER_SEPARATION";
    private static final Pattern PROMPT_VARIABLE_PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final Set<String> ALLOWED_PROMPT_VARIABLES = Set.of("title", "analysisType", "transcript");
    private static final Set<String> SUMMARY_ANALYSIS_TYPES =
            Set.of(
                    ANALYSIS_TYPE_GENERAL_MEETING,
                    ANALYSIS_TYPE_DEV_MEETING,
                    ANALYSIS_TYPE_CONSULTING_MEETING
            );

    private final MeetingTemplateRepository meetingTemplateRepository;
    private final CodeItemRepository codeItemRepository;
    private final UserTemplatePermissionRepository userTemplatePermissionRepository;

    public List<MeetingTemplateDto> getActiveTemplates(String loginEmail) {
        List<MeetingTemplate> templates =
                meetingTemplateRepository.findByTemplateCategoryCodeAndActiveOrderBySortOrderAscTemplateNameAsc(
                        CATEGORY_ANALYSIS_SUMMARY,
                        true
                );

        if (loginEmail == null || loginEmail.isBlank() || "en91neer@naver.com".equalsIgnoreCase(loginEmail)) {
            return templates.stream().map(MeetingTemplateDto::new).toList();
        }

        Set<String> allowedCodes =
                userTemplatePermissionRepository
                        .findByUserEmailIgnoreCase(loginEmail)
                        .stream()
                        .map(UserTemplatePermission::getAnalysisTypeCode)
                        .collect(Collectors.toSet());

        if (allowedCodes.isEmpty()) {
            return templates.stream().map(MeetingTemplateDto::new).toList();
        }

        return templates
                .stream()
                .filter(template -> allowedCodes.contains(template.getAnalysisTypeCode()))
                .map(MeetingTemplateDto::new)
                .toList();
    }

    public List<MeetingTemplateDto> getAllTemplates() {
        return meetingTemplateRepository
                .findAllByOrderBySortOrderAscTemplateNameAsc()
                .stream()
                .map(MeetingTemplateDto::new)
                .toList();
    }

    @Transactional
    public MeetingTemplateDto save(MeetingTemplateSaveRequestDto dto) {
        MeetingTemplate template =
                dto.getId() == null
                        ? new MeetingTemplate()
                        : meetingTemplateRepository
                                .findById(dto.getId())
                                .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다."));

        String templateCategoryCode =
                dto.getTemplateCategoryCode() == null || dto.getTemplateCategoryCode().isBlank()
                        ? CATEGORY_ANALYSIS_SUMMARY
                        : dto.getTemplateCategoryCode().trim();
        String analysisTypeCode = require(dto.getAnalysisTypeCode(), "분석유형 코드를 선택해주세요.");
        validateTemplateCategoryAndAnalysisType(dto.getId(), templateCategoryCode, analysisTypeCode);

        CodeItem analysisType =
                codeItemRepository
                        .findByGroupCodeAndCode("ANALYSIS_TYPE", analysisTypeCode)
                        .orElseThrow(() -> new RuntimeException("분석유형 코드를 찾을 수 없습니다."));

        template.setAnalysisTypeCode(analysisTypeCode);
        template.setTemplateCategoryCode(templateCategoryCode);
        template.setTemplateName(analysisType.getCodeName());
        String systemPromptTemplate = require(dto.getSystemPromptTemplate(), "System Prompt를 입력해주세요.");
        String userPromptTemplate = require(dto.getUserPromptTemplate(), "User Prompt를 입력해주세요.");
        validatePromptVariables(systemPromptTemplate);
        validatePromptVariables(userPromptTemplate);

        template.setModelType(require(dto.getModelType(), "분석 모델 타입을 입력해주세요."));
        template.setSystemPromptTemplate(systemPromptTemplate);
        template.setUserPromptTemplate(userPromptTemplate);
        template.setSpeakerAnalysisEnabled(dto.getSpeakerAnalysisEnabled() == null || dto.getSpeakerAnalysisEnabled());
        template.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        template.setActive(dto.getActive() == null || dto.getActive());

        MeetingTemplate savedTemplate = meetingTemplateRepository.save(template);
        if (!savedTemplate.getActive()) {
            userTemplatePermissionRepository.deleteByAnalysisTypeCode(savedTemplate.getAnalysisTypeCode());
        }

        return new MeetingTemplateDto(savedTemplate);
    }

    @Transactional
    public void delete(Long id) {
        MeetingTemplate template =
                meetingTemplateRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다."));

        userTemplatePermissionRepository.deleteByAnalysisTypeCode(template.getAnalysisTypeCode());
        meetingTemplateRepository.delete(template);
    }

    private String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }

        return value.trim();
    }

    private void validateTemplateCategoryAndAnalysisType(
            Long templateId,
            String templateCategoryCode,
            String analysisTypeCode
    ) {
        if (CATEGORY_ANALYSIS_SUMMARY.equals(templateCategoryCode)) {
            if (!SUMMARY_ANALYSIS_TYPES.contains(analysisTypeCode)) {
                throw new RuntimeException("회의요약 템플릿은 일반회의, 개발회의, 상담회의만 선택할 수 있습니다.");
            }
        } else if (CATEGORY_SPEAKER_SEPARATION.equals(templateCategoryCode)) {
            if (!ANALYSIS_TYPE_SPEAKER_SEPARATION.equals(analysisTypeCode)) {
                throw new RuntimeException("화자분리 템플릿은 화자분리 분석유형만 선택할 수 있습니다.");
            }

            boolean hasOtherSpeakerTemplate =
                    meetingTemplateRepository
                            .findByTemplateCategoryCode(CATEGORY_SPEAKER_SEPARATION)
                            .stream()
                            .anyMatch(template -> !template.getId().equals(templateId));

            if (hasOtherSpeakerTemplate) {
                throw new RuntimeException("화자분리 템플릿은 한 개만 등록할 수 있습니다.");
            }
        } else {
            throw new RuntimeException("지원하지 않는 템플릿 구분입니다.");
        }

        meetingTemplateRepository
                .findByTemplateCategoryCodeAndAnalysisTypeCode(templateCategoryCode, analysisTypeCode)
                .stream()
                .filter(template -> !template.getId().equals(templateId))
                .findFirst()
                .ifPresent(template -> {
                    throw new RuntimeException("이미 등록된 분석유형입니다.");
                });
    }

    private void validatePromptVariables(String prompt) {
        Matcher matcher = PROMPT_VARIABLE_PATTERN.matcher(prompt);
        Set<String> invalidVariables =
                matcher
                        .results()
                        .map(match -> match.group(1))
                        .filter(variable -> !ALLOWED_PROMPT_VARIABLES.contains(variable))
                        .collect(Collectors.toSet());

        if (!invalidVariables.isEmpty()) {
            String invalidText =
                    invalidVariables
                            .stream()
                            .sorted()
                            .map(variable -> "{" + variable + "}")
                            .collect(Collectors.joining(", "));
            throw new RuntimeException("사용할 수 없는 치환값입니다: " + invalidText);
        }
    }
}
