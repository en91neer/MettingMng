package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.entity.CodeGroup;
import com.example.demo.entity.CodeItem;
import com.example.demo.entity.MeetingTemplate;
import com.example.demo.repository.CodeGroupRepository;
import com.example.demo.repository.CodeItemRepository;
import com.example.demo.repository.MeetingTemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReferenceDataInitializer implements CommandLineRunner {

    private final CodeGroupRepository codeGroupRepository;
    private final CodeItemRepository codeItemRepository;
    private final MeetingTemplateRepository meetingTemplateRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        repairLegacyMeetingTemplateSchema();

        seedGroup("FILE_TARGET_TYPE", "파일 업무구분", "첨부 파일이 연결되는 업무 도메인");
        seedGroup("ANALYSIS_TYPE", "분석 유형", "회의록 분석 템플릿 유형");
        seedGroup("TEMPLATE_CATEGORY", "템플릿 구분", "프롬프트 템플릿 사용 목적");
        seedGroup("AI_MODEL_TYPE", "AI 모델 타입", "OpenAI 분석 모델");
        seedGroup("USER_ROLE", "사용자 권한", "서비스 이용 권한");
        seedGroup("USER_STATUS", "사용자 상태", "가입 승인 상태");

        seedCode("FILE_TARGET_TYPE", "MEETING_MINUTES_AUDIO", "회의록 음성파일", 10);
        seedCode("ANALYSIS_TYPE", "GENERAL_MEETING", "일반회의", 10);
        seedCode("ANALYSIS_TYPE", "DEV_MEETING", "개발회의", 20);
        seedCode("ANALYSIS_TYPE", "CONSULTING_MEETING", "상담회의", 30);
        seedCode("ANALYSIS_TYPE", "SPEAKER_SEPARATION", "화자분리", 90);
        seedCode("TEMPLATE_CATEGORY", "ANALYSIS_SUMMARY", "회의요약", 10);
        seedCode("TEMPLATE_CATEGORY", "SPEAKER_SEPARATION", "화자분리", 20);
        seedCode("AI_MODEL_TYPE", "gpt-4.1-mini", "gpt-4.1-mini", 10);
        seedCode("AI_MODEL_TYPE", "gpt-4.1", "gpt-4.1", 20);
        seedCode("AI_MODEL_TYPE", "gpt-4o-mini", "gpt-4o-mini", 30);
        seedCode("USER_ROLE", "FREE_USER", "무료사용자", 10);
        seedCode("USER_ROLE", "PREMIUM_USER", "프리미엄", 20);
        seedCode("USER_ROLE", "SUPER_USER", "슈퍼유저", 30);
        seedCode("USER_STATUS", "PENDING", "승인대기", 10);
        seedCode("USER_STATUS", "ACTIVE", "정상", 20);
        seedCode("USER_STATUS", "SUSPENDED", "중지", 30);

        seedTemplate("ANALYSIS_SUMMARY", "GENERAL_MEETING", "일반회의", "gpt-4.1-mini", defaultSummarySystemPrompt(), defaultSummaryUserPrompt(), 10);
        seedTemplate("ANALYSIS_SUMMARY", "DEV_MEETING", "개발회의", "gpt-4.1-mini", defaultSummarySystemPrompt() + "\n- 개발회의는 API, DB, 배포, 장애, 담당 작업을 우선 정리한다.", defaultSummaryUserPrompt(), 20);
        seedTemplate("ANALYSIS_SUMMARY", "CONSULTING_MEETING", "상담회의", "gpt-4.1-mini", defaultSummarySystemPrompt() + "\n- 상담회의는 고객 요구사항, 불만, 약속사항, 후속 연락을 우선 정리한다.", defaultSummaryUserPrompt(), 30);
        seedTemplate("SPEAKER_SEPARATION", "SPEAKER_SEPARATION", "화자분리", "gpt-4.1-mini", defaultSpeakerSystemPrompt(), defaultSpeakerUserPrompt(), 90);
    }

    private void repairLegacyMeetingTemplateSchema() {
        Integer legacyColumnCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_name = 'meeting_template'
                  and column_name = 'prompt_template'
                """, Integer.class);

        if (legacyColumnCount != null && legacyColumnCount > 0) {
            jdbcTemplate.execute("alter table if exists meeting_template alter column prompt_template drop not null");
            jdbcTemplate.update("""
                    update meeting_template
                    set prompt_template = coalesce(system_prompt_template, '템플릿 관리 화면에서 System Prompt를 입력해주세요.')
                    where prompt_template is null
                    """);
        }
    }

    private void seedGroup(String groupCode, String groupName, String description) {
        codeGroupRepository
                .findByGroupCode(groupCode)
                .orElseGet(() ->
                        codeGroupRepository.save(
                                CodeGroup.builder()
                                        .groupCode(groupCode)
                                        .groupName(groupName)
                                        .description(description)
                                        .build()
                        )
                );
    }

    private void seedCode(String groupCode, String code, String codeName, int sortOrder) {
        codeItemRepository
                .findByGroupCodeAndCode(groupCode, code)
                .orElseGet(() ->
                        codeItemRepository.save(
                                CodeItem.builder()
                                        .groupCode(groupCode)
                                        .code(code)
                                        .codeName(codeName)
                                        .sortOrder(sortOrder)
                                        .build()
                        )
                );
    }

    private void seedTemplate(String categoryCode, String code, String name, String modelType, String systemPrompt, String userPrompt, int sortOrder) {
        meetingTemplateRepository
                .findByAnalysisTypeCode(code)
                .ifPresentOrElse(template -> {
                    if (template.getTemplateCategoryCode() == null || template.getTemplateCategoryCode().isBlank()) {
                        template.setTemplateCategoryCode(categoryCode);
                    }
                    if (template.getSystemPromptTemplate() == null || template.getSystemPromptTemplate().isBlank()) {
                        template.setSystemPromptTemplate(systemPrompt);
                    }
                    if (template.getUserPromptTemplate() == null || template.getUserPromptTemplate().isBlank()) {
                        template.setUserPromptTemplate(userPrompt);
                    }
                    if (template.getModelType() == null || template.getModelType().isBlank()) {
                        template.setModelType(modelType);
                    }
                    meetingTemplateRepository.save(template);
                }, () ->
                        meetingTemplateRepository.save(
                                MeetingTemplate.builder()
                                        .analysisTypeCode(code)
                                        .templateCategoryCode(categoryCode)
                                        .templateName(name)
                                        .modelType(modelType)
                                        .systemPromptTemplate(systemPrompt)
                                        .userPromptTemplate(userPrompt)
                                        .speakerAnalysisEnabled(true)
                                        .sortOrder(sortOrder)
                                        .build()
                        )
                );
    }

    private String defaultSummarySystemPrompt() {
        return """
                당신은 회의 내용을 간결하게 정리하는 AI 회의록 작성자입니다.

                치환값:
                - 회의 제목: {title}
                - 회의 종류: {analysisType}
                - 화자분리 전사문: {transcript}

                공통 규칙:
                - 결과 제목은 반드시 [회의 종류] 한 줄만 사용한다.
                - 전체 내용은 심플하게 요약한다.
                - 확인되지 않은 내용은 추측하지 않는다.
                - 섹션은 "핵심", "결정", "할 일", "용어 설명"만 사용한다.
                - 섹션 제목 바로 다음 줄부터 내용을 작성하고, 제목 다음에 빈 줄을 넣지 않는다.
                - "핵심" 섹션 제목 다음에는 반드시 바로 첫 문장을 작성한다.
                - 대화록 전문과 [대화록] 섹션은 출력하지 않는다.
                - 전문용어가 있으면 "용어 설명"에 [1] 용어: 설명 형식으로 짧게 적는다.
                - 설명할 전문용어가 없으면 "용어 설명" 섹션에 "없음"이라고 작성한다.
                """;
    }

    private String defaultSummaryUserPrompt() {
        return """
                회의 제목: {title}
                회의 종류: {analysisType}

                아래 화자분리 전사문을 참고해서 심플하게 요약해줘.
                내용을 추측하지 말고, 전사문에서 확인되는 내용만 사용해줘.
                대화 중 전문용어가 있으면 본문 하단 "용어 설명"에 각주 형식으로 간략히 설명해줘.

                화자분리 전사문:
                {transcript}
                """;
    }

    private String defaultSpeakerSystemPrompt() {
        return """
                당신은 회의 음성 전사문을 화자별 대화록으로 정리하는 전문가입니다.

                규칙:
                1. 원문에 명시되지 않은 이름은 추측하지 말고 화자1, 화자2 형식으로 표기한다.
                2. 발언 순서는 유지한다.
                3. 의미를 바꾸지 말고 문장만 자연스럽게 다듬는다.
                4. 불명확한 발언은 [불명확]으로 표시한다.
                5. 결과는 아래 형식만 사용한다.

                [화자분리 전사문]
                화자1: 발언
                화자2: 발언
                """;
    }

    private String defaultSpeakerUserPrompt() {
        return """
                다음 전사문을 화자별 대화록으로 정리해줘.

                {transcript}
                """;
    }
}
