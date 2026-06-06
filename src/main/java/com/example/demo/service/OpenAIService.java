package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.MessageAI.ChatResponse;
import com.example.demo.dto.WhisperAI.WhisperResponse;
import com.example.demo.entity.AnalysisResult;
import com.example.demo.entity.AnalyzeStatus;
import com.example.demo.entity.FileAttach;
import com.example.demo.entity.MeetingTemplate;
import com.example.demo.entity.UserActionLog;
import com.example.demo.repository.AnalysisResultRepository;
import com.example.demo.repository.FileAttachRepository;
import com.example.demo.repository.MeetingTemplateRepository;
import com.example.demo.repository.UserActionLogRepository;
import com.example.demo.websocket.StatsWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

	// 분석 요약 템플릿을 조회할 때 사용하는 공통코드 그룹값이다.
	private static final String TEMPLATE_CATEGORY_ANALYSIS_SUMMARY = "ANALYSIS_SUMMARY";

	// 화자분리 전용 템플릿을 조회할 때 사용하는 공통코드 값이다.
	private static final String TEMPLATE_CODE_SPEAKER_SEPARATION = "SPEAKER_SEPARATION";

	// OpenAI Audio Transcriptions API: 음성 파일을 텍스트 전사문으로 변환한다.
	private static final String OPENAI_AUDIO_TRANSCRIPTIONS_API_URL = "https://api.openai.com/v1/audio/transcriptions";

	// OpenAI Chat Completions API: 화자분리와 회의 요약을 생성한다.
	private static final String OPENAI_CHAT_COMPLETIONS_API_URL = "https://api.openai.com/v1/chat/completions";

	// 한글/영문 혼합 문장을 대략적으로 토큰 계산할 때 사용하는 추정 비율이다.
	private static final int ESTIMATED_CHARS_PER_TOKEN = 4;

	// 첨부파일 정보와 분석 상태를 저장한다.
	private final FileAttachRepository fileAttachRepository;

	// 분석 유형별 회의록 요약 결과를 저장한다.
	private final AnalysisResultRepository analysisResultRepository;

	// 분석 실행 이력을 남긴다.
	private final UserActionLogRepository userActionLogRepository;

	// 화면 분석 게이지를 실시간으로 갱신한다.
	private final StatsWebSocketHandler statsWebSocketHandler;

	// DB에서 분석/화자분리 프롬프트 템플릿을 조회한다.
	private final MeetingTemplateRepository meetingTemplateRepository;

	// 서버 기동 후 당일 OpenAI 예상 사용량을 메모리에서 누적 관리한다.
	private final AtomicInteger estimatedTokensUsedToday = new AtomicInteger(0);

	// 예상 토큰 사용량을 날짜가 바뀔 때 0으로 초기화하기 위한 기준일이다.
	private LocalDate tokenBudgetDate = LocalDate.now();

	// OpenAI API Key. 운영에서는 OPENAI_API_KEY 환경변수로 주입한다.
	@Value("${openai.api-key:}")
	private String openAiApiKey;

	// 음성/전사문/요약 파일이 저장되는 최상위 경로이다.
	@Value("${file.upload-root-path:c:/dog-walk-nas/upload/}")
	private String rootPath;

	// Chat API 1회 호출당 허용할 최대 예상 토큰 수이다. 0 이하면 제한하지 않는다.
	@Value("${openai.max-estimated-tokens-per-call:12000}")
	private int maxEstimatedTokensPerCall;

	// 하루 동안 허용할 최대 예상 토큰 수이다. 0 이하면 제한하지 않는다.
	@Value("${openai.max-estimated-tokens-per-day:30000}")
	private int maxEstimatedTokensPerDay;

	// Chat API 응답으로 생성될 최대 토큰 수이다.
	@Value("${openai.max-estimated-output-tokens-per-chat:2000}")
	private int maxEstimatedOutputTokensPerChat;

	// 1. 전체 분석 흐름: 파일 확인, 음성 전사, 화자분리, 요약, 저장, 진행률 갱신을 순서대로 처리한다.
	@Async
	@Transactional
	public void openAiAnalyze(String title, Long fileId, String analysisType, String loginEmail) throws IOException {
		MeetingTemplate template = resolveMeetingTemplate(analysisType);
		String analysisTypeCode = template.getAnalysisTypeCode();
		String analysisTypeName = template.getTemplateName();
		boolean speakerAnalysisEnabled =
				template.getSpeakerAnalysisEnabled() == null || template.getSpeakerAnalysisEnabled();
		
		// 파일정보를 가져온다.
		FileAttach fileAttach = fileAttachRepository.findById(fileId).orElseThrow();
		validateFileOwner(fileAttach, loginEmail);
		fileAttach.setAnalyzeStatus(AnalyzeStatus.WAITING);
		statsWebSocketHandler.sendAnalyzeProgress(fileId, 10, AnalyzeStatus.WAITING.name(), "분석 대기중");

		try {
			fileAttach.setAnalyzeStatus(AnalyzeStatus.PROCESSING);
			fileAttachRepository.save(fileAttach);
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 20, AnalyzeStatus.PROCESSING.name(), "분석 시작");

			File file = new File(fileAttach.getAudioFilePath());
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 30, AnalyzeStatus.PROCESSING.name(), "음성 파일 확인");
			
			// 음성파일에서 텍스트파일 추출 AI수행
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 40, AnalyzeStatus.PROCESSING.name(), "음성 텍스트 추출 중");
			LocalDate today = LocalDate.now();
			if (!today.equals(tokenBudgetDate)) {
				tokenBudgetDate = today;
				estimatedTokensUsedToday.set(0);
			}
			if (maxEstimatedTokensPerDay > 0 && estimatedTokensUsedToday.get() >= maxEstimatedTokensPerDay) {
				throw new IllegalStateException("OpenAI 음성 전사 호출을 차단했습니다. 일일 예상 토큰 한도를 모두 사용했습니다.");
			}

			WhisperResponse whisperResponse = this.getVoiceToText(file);
			if (whisperResponse == null || whisperResponse.getText() == null) {
				throw new IllegalStateException("음성 텍스트 추출 실패");
			}
			String transcript = whisperResponse.getText();
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 50, AnalyzeStatus.PROCESSING.name(), "음성 텍스트 추출 완료");

			String transcriptForAnalysis = transcript;
			if (speakerAnalysisEnabled) {
				statsWebSocketHandler.sendAnalyzeProgress(fileId, 55, AnalyzeStatus.PROCESSING.name(), "화자분리 중");
				transcriptForAnalysis = this.getSpeakerSeparatedText(transcript);
				if (transcriptForAnalysis == null || transcriptForAnalysis.isBlank()) {
					throw new IllegalStateException("화자분리 실패");
				}
				statsWebSocketHandler.sendAnalyzeProgress(fileId, 58, AnalyzeStatus.PROCESSING.name(), "화자분리 완료");
			} else {
				statsWebSocketHandler.sendAnalyzeProgress(fileId, 58, AnalyzeStatus.PROCESSING.name(), "화자분리 없이 전사문 사용");
			}
			
			// 결과를 파일로 저장한다.
			String transcriptPath = saveTextFile(
					fileId,
					"transcript",
					transcriptForAnalysis,
					loginEmail
			);
			fileAttach.setTranscriptFilePath(transcriptPath);
			fileAttachRepository.save(fileAttach);	//DB 파일경로 업데이트 처리
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 60, AnalyzeStatus.PROCESSING.name(), "전사문 파일 저장 완료");
	
			// 추출한 텍스트파일 원본에서 요약된 텍스트 AI수행
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 70, AnalyzeStatus.PROCESSING.name(), analysisTypeName + " 형식 회의록 요약 중");
			ChatResponse chatResponse = this.getTextToAnalyze(
					title,
					transcriptForAnalysis,
					template
			);
			if (
					chatResponse == null
					|| chatResponse.getChoices() == null
					|| chatResponse.getChoices().isEmpty()
					|| chatResponse.getChoices().get(0).getMessage() == null
			) {
				throw new IllegalStateException("회의록 요약 생성 실패");
			}

			String summaryText = chatResponse
					.getChoices()
					.get(0)
					.getMessage()
					.getContent();
			summaryText = normalizeSummaryTitle(summaryText, analysisTypeName);
			summaryText = appendConversationTranscript(summaryText, transcriptForAnalysis);
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 80, AnalyzeStatus.PROCESSING.name(), "회의록 요약 완료");
			
			// 결과를 파일로 저장한다.
			String summaryPath = saveTextFile(
					fileId,
					"summary",
					summaryText,
					loginEmail
			);
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 90, AnalyzeStatus.PROCESSING.name(), "요약 파일 저장 완료");
			saveAnalysisResult(fileAttach, analysisTypeCode, summaryText, loginEmail);
			
			fileAttach.setSummaryFilePath(summaryPath);
			fileAttach.setAnalyzeStatus(AnalyzeStatus.COMPLETE);
			fileAttachRepository.save(fileAttach);	//DB 파일경로 업데이트 처리
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 100, AnalyzeStatus.COMPLETE.name(), "분석 완료");
		} catch(Exception e) {
			log.error("익셉션 발생={}", e.getMessage());
			fileAttach.setAnalyzeStatus(AnalyzeStatus.FAIL);
			fileAttachRepository.save(fileAttach);
			String failMessage = "분석 실패";
			String exceptionMessage = e.getMessage();
			if (
					exceptionMessage != null
					&& (
							exceptionMessage.contains("일일 예상 토큰 한도")
							|| exceptionMessage.contains("1회 한도")
							|| exceptionMessage.contains("일일 예상 토큰 한도를 모두 사용")
					)
			) {
				failMessage = "한도가 소진되어 분석이 불가합니다.";
			}
			statsWebSocketHandler.sendAnalyzeProgress(fileId, 0, AnalyzeStatus.FAIL.name(), failMessage);
		}
	}

	// 2-1. 요약 결과 맨 위 제목을 선택한 분석유형 제목 하나만 남기도록 정리한다.
	private String normalizeSummaryTitle(String summaryText, String analysisType) {
		String title = "[" + analysisType + "]";

		if (summaryText == null || summaryText.isBlank()) {
			return title;
		}

		String content =
				summaryText
						.strip()
						.replaceFirst(
								"(?s)^(?:(?:\\*\\*)?\\[[^\\]]+\\](?:\\*\\*)?\\s*)+",
								""
						)
						.strip();

		return """
				%s

				%s
		""".formatted(title, content);
	}

	// 2-2. 최종 요약 하단에 화자분리된 대화록을 붙이고, 화자가 바뀔 때 한 줄을 비운다.
	private String appendConversationTranscript(String summaryText, String speakerSeparatedTranscript) {
		if (speakerSeparatedTranscript == null || speakerSeparatedTranscript.isBlank()) {
			return summaryText;
		}

		String normalizedTranscript =
				speakerSeparatedTranscript
						.replace("[화자분리 전사문]", "")
						.replaceAll("\\R(?=화자\\d+\\s*:)", "\n\n")
						.replaceAll("\\n{3,}", "\n\n")
						.strip();

		if (summaryText == null || summaryText.isBlank()) {
			return """
					[대화록]

					%s
					""".formatted(normalizedTranscript);
		}

		if (summaryText.contains("[대화록]")) {
			return summaryText;
		}

		return """
				%s

				[대화록]

				%s
				""".formatted(summaryText.strip(), normalizedTranscript);
	}

	// 3-1. 분석 결과를 DB에 저장하고 분석 실행 이력을 남긴다.
	private void saveAnalysisResult(
			FileAttach fileAttach,
			String analysisTypeCode,
			String summaryText,
			String loginEmail
	) {
		AnalysisResult analysisResult =
				analysisResultRepository
						.findByFileIdAndAnalysisTypeCode(
								fileAttach.getId(),
								analysisTypeCode
						)
						.orElseGet(() ->
								AnalysisResult.builder()
										.meetingMinutesId(fileAttach.getTargetId())
										.fileId(fileAttach.getId())
										.analysisTypeCode(analysisTypeCode)
										.build()
						);

		analysisResult.setContent(summaryText);
		analysisResult.setUpdatedBy(loginEmail);

		if (analysisResult.getCreatedBy() == null) {
			analysisResult.setCreatedBy(loginEmail);
		}

		AnalysisResult savedAnalysisResult = analysisResultRepository.save(analysisResult);

		UserActionLog log = new UserActionLog();
		log.setLoginEmail(loginEmail);
		log.setActionType("ANALYZE");
		log.setTargetType("ANALYSIS_RESULT");
		log.setTargetId(savedAnalysisResult.getId());
		userActionLogRepository.save(log);
	}

	// 3-2. 전사문/요약 내용을 로그인 사용자별 날짜 폴더에 텍스트 파일로 저장한다.
	public String saveTextFile(
			Long fileId,
			String prefix,
			String content,
			String loginEmail
	) throws IOException {

		LocalDate now = LocalDate.now();
		Path uploadPath = Paths.get(
				rootPath,
				sanitizePathSegment(loginEmail),
				String.valueOf(now.getYear()),
				String.format("%02d", now.getMonthValue()),
				String.format("%02d", now.getDayOfMonth())
		);
		Files.createDirectories(uploadPath);

		String fileName =
				sanitizePathSegment(prefix)
				+ "_"
				+ fileId
				+ ".txt";
		String fullPath =
				uploadPath
						.resolve(fileName)
						.toString();
		Files.writeString(Paths.get(fullPath), content, StandardCharsets.UTF_8);

		return fullPath;
	}

	// 4-1. OpenAI 음성 전사 API를 호출해서 음성파일을 일반 텍스트로 변환한다.
	public WhisperResponse getVoiceToText(File file) throws IOException {

		WhisperResponse whisperResponse = null;

		log.info("파일명={}", file.getName());
		log.info("존재여부={}", file.exists());
		log.info("크기={}", file.length());
		log.info("경로={}", file.getAbsolutePath());
		log.info("파일크기={}", file.length());
		log.info("MINE Type={}", Files.probeContentType(file.toPath()));

		FileSystemResource resource = new FileSystemResource(file);
		log.info("resource=" + resource.contentLength());

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(getOpenAiApiKey());
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

		// body.add("model", "whisper-1");
		body.add("model", "gpt-4o-transcribe");
		body.add("file", resource);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(OPENAI_AUDIO_TRANSCRIPTIONS_API_URL,
					HttpMethod.POST, request, String.class);

			String result = response.getBody();
			log.info("getVoiceToText 원본 JSON={}", result);

			// String -> Object
			ObjectMapper mapper = new ObjectMapper();
			whisperResponse = mapper.readValue(result, WhisperResponse.class);
		} catch (HttpClientErrorException e) {
			log.error("에러응답={}", e.getResponseBodyAsString());
		}

		return whisperResponse;
	}

	// 4-2. DB의 화자분리 템플릿으로 전사문을 화자1, 화자2 형식의 대화록으로 정리한다.
	public String getSpeakerSeparatedText(String transcript) {

		//화자분리 템플릿을 가져온다.
		MeetingTemplate template = resolveSpeakerSeparationTemplate();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> body = new HashMap<>();
		List<Map<String, String>> messages = new ArrayList<>();
		ChatResponse chatResponse = null;

		if (template.getSystemPromptTemplate() == null || template.getSystemPromptTemplate().isBlank()) {
			throw new IllegalStateException("템플릿 관리 화면에서 System Prompt를 입력해주세요.");
		}
		if (template.getUserPromptTemplate() == null || template.getUserPromptTemplate().isBlank()) {
			throw new IllegalStateException("템플릿 관리 화면에서 User Prompt를 입력해주세요.");
		}

		String systemPrompt = renderPromptTemplate(
				template.getSystemPromptTemplate(),
				"",
				template.getTemplateName(),
				transcript
		);
		String userPrompt = renderPromptTemplate(
				template.getUserPromptTemplate(),
				"",
				template.getTemplateName(),
				transcript
		);

		headers.setBearerAuth(getOpenAiApiKey());
		headers.setContentType(MediaType.APPLICATION_JSON);
		body.put("model", template.getModelType());
		if (maxEstimatedOutputTokensPerChat > 0) {
			body.put("max_tokens", maxEstimatedOutputTokensPerChat);
		}

		try {
			//예상토큰을 체크한다.
			validateOpenAiChatTokenBudget("화자분리", systemPrompt, userPrompt);

			messages.add(Map.of("role", "system", "content", systemPrompt));
			messages.add(Map.of("role", "user", "content", userPrompt));

			body.put("messages", messages);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.exchange(OPENAI_CHAT_COMPLETIONS_API_URL,
					HttpMethod.POST, request, String.class);
			HttpHeaders responseHeaders = response.getHeaders();
			String limitTokens = responseHeaders.getFirst("x-ratelimit-limit-tokens");
			String remainingTokens = responseHeaders.getFirst("x-ratelimit-remaining-tokens");
			String resetTokens = responseHeaders.getFirst("x-ratelimit-reset-tokens");
			if (limitTokens != null || remainingTokens != null || resetTokens != null) {
				log.info(
						"OpenAI {} rate-limit tokens limit={}, remaining={}, reset={}",
						"화자분리",
						limitTokens,
						remainingTokens,
						resetTokens
				);
			}

			String result = response.getBody();
			log.info("getSpeakerSeparatedText 원본 JSON={}", result);

			ObjectMapper mapper = new ObjectMapper();
			chatResponse = mapper.readValue(result, ChatResponse.class);
		} catch (HttpClientErrorException e) {
			log.error("화자분리 에러응답={}", e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error("화자분리 예외발생={}", e.getMessage());
		}

		if (chatResponse == null || chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
			return null;
		}

		return chatResponse.getChoices().get(0).getMessage().getContent();
	}

	// 4-3. DB의 분석 템플릿으로 화자분리 대화록을 선택한 분석유형에 맞게 요약한다.
	public ChatResponse getTextToAnalyze(String title, String transcript, MeetingTemplate template) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> body = new HashMap<>();
		List<Map<String, String>> messages = new ArrayList<>();
		ChatResponse chatResponse = null;

		headers.setBearerAuth(getOpenAiApiKey());
		headers.setContentType(MediaType.APPLICATION_JSON);
		body.put("model", template.getModelType());
		if (maxEstimatedOutputTokensPerChat > 0) {
			body.put("max_tokens", maxEstimatedOutputTokensPerChat);
		}

		try {
			if (template.getSystemPromptTemplate() == null || template.getSystemPromptTemplate().isBlank()) {
				throw new IllegalStateException("템플릿 관리 화면에서 System Prompt를 입력해주세요.");
			}
			if (template.getUserPromptTemplate() == null || template.getUserPromptTemplate().isBlank()) {
				throw new IllegalStateException("템플릿 관리 화면에서 User Prompt를 입력해주세요.");
			}

			String systemPrompt = renderPromptTemplate(
					template.getSystemPromptTemplate(),
					title,
					template.getTemplateName(),
					transcript
			);
			String userPrompt = renderPromptTemplate(
					template.getUserPromptTemplate(),
					title,
					template.getTemplateName(),
					transcript
			);

			validateOpenAiChatTokenBudget("회의 요약", systemPrompt, userPrompt);

			messages.add(Map.of("role", "system", "content", systemPrompt));
			messages.add(Map.of("role", "user", "content", userPrompt));

			body.put("messages", messages);

			HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.exchange(OPENAI_CHAT_COMPLETIONS_API_URL,
					HttpMethod.POST, request1, String.class);
			HttpHeaders responseHeaders = response.getHeaders();
			String limitTokens = responseHeaders.getFirst("x-ratelimit-limit-tokens");
			String remainingTokens = responseHeaders.getFirst("x-ratelimit-remaining-tokens");
			String resetTokens = responseHeaders.getFirst("x-ratelimit-reset-tokens");
			if (limitTokens != null || remainingTokens != null || resetTokens != null) {
				log.info(
						"OpenAI {} rate-limit tokens limit={}, remaining={}, reset={}",
						"회의 요약",
						limitTokens,
						remainingTokens,
						resetTokens
				);
			}

			String result = response.getBody();
			log.info("getTextToAnalyze 원본 JSON={}", result);

			ObjectMapper mapper = new ObjectMapper();
			chatResponse = mapper.readValue(result, ChatResponse.class);
			String summary = chatResponse.getChoices().get(0).getMessage().getContent();

			log.info("getTextToAnalyze Content={}", summary);
		} catch (HttpClientErrorException e) {
			log.error("에러응답={}", e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error("예외발생={}", e.getMessage());
		}

		return chatResponse;
	}

	// 5-1. Chat API 호출 전 예상 토큰을 계산해서 1회/일일 한도 초과 시 호출을 차단한다.
	private synchronized void validateOpenAiChatTokenBudget(String apiName, String... contents) {
		LocalDate today = LocalDate.now();
		if (!today.equals(tokenBudgetDate)) {
			tokenBudgetDate = today;
			estimatedTokensUsedToday.set(0);
		}

		int totalCharacters = 0;
		if (contents != null) {
			for (String content : contents) {
				if (content != null) {
					totalCharacters += content.length();
				}
			}
		}
		int estimatedTokens =
				Math.max(1, (int) Math.ceil((double) totalCharacters / ESTIMATED_CHARS_PER_TOKEN))
				+ Math.max(maxEstimatedOutputTokensPerChat, 0);

		if (maxEstimatedTokensPerCall > 0 && estimatedTokens > maxEstimatedTokensPerCall) {
			throw new IllegalStateException(
					"OpenAI " + apiName + " 예상 토큰이 1회 한도를 초과했습니다. estimatedTokens="
							+ estimatedTokens
							+ ", maxEstimatedTokensPerCall="
							+ maxEstimatedTokensPerCall
			);
		}

		if (maxEstimatedTokensPerDay > 0) {
			int usedTokens = estimatedTokensUsedToday.get();
			int remainingTokens = maxEstimatedTokensPerDay - usedTokens;

			if (estimatedTokens > remainingTokens) {
				throw new IllegalStateException(
						"OpenAI " + apiName + " 일일 예상 토큰 한도를 초과하여 호출을 차단했습니다. estimatedTokens="
								+ estimatedTokens
								+ ", remainingTokens="
								+ Math.max(remainingTokens, 0)
								+ ", maxEstimatedTokensPerDay="
								+ maxEstimatedTokensPerDay
				);
			}
		}

		int usedAfterCall = estimatedTokensUsedToday.addAndGet(estimatedTokens);
		int remainingAfterCall =
				maxEstimatedTokensPerDay > 0
						? Math.max(maxEstimatedTokensPerDay - usedAfterCall, 0)
						: -1;

		log.info(
				"OpenAI {} 예상 토큰 체크 완료 estimatedTokens={}, usedToday={}, remainingToday={}",
				apiName,
				estimatedTokens,
				usedAfterCall,
				remainingAfterCall >= 0 ? remainingAfterCall : "unlimited"
		);
	}

	// 5-2. 윈도우/리눅스 파일명에 사용할 수 없는 문자를 안전한 문자로 바꾼다.
	private String sanitizePathSegment(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}

		return value.replaceAll("[\\\\/:*?\"<>|]", "_").strip();
	}

	// 5-3. OpenAI API Key가 비어 있으면 외부 호출 전에 명확한 오류를 발생시킨다.
	private String getOpenAiApiKey() {
		if (openAiApiKey == null || openAiApiKey.isBlank()) {
			throw new IllegalStateException("openai.api-key 설정값이 없습니다.");
		}

		return openAiApiKey;
	}

	// 5-4. 로그인 사용자가 본인이 업로드한 파일만 분석할 수 있도록 검증한다.
	private void validateFileOwner(FileAttach fileAttach, String loginEmail) {
		if (loginEmail == null || loginEmail.isBlank()) {
			throw new RuntimeException("로그인 정보가 없습니다.");
		}

		if (!loginEmail.equals(fileAttach.getCreatedBy())) {
			throw new RuntimeException("본인 파일만 분석할 수 있습니다.");
		}
	}

	// 5-5. 사용자가 선택한 분석유형 코드/이름으로 활성화된 회의요약 템플릿을 찾는다.
	private MeetingTemplate resolveMeetingTemplate(String analysisType) {
		String keyword = analysisType == null ? "" : analysisType.trim();

		if (!keyword.isBlank()) {
			return meetingTemplateRepository
					.findByTemplateCategoryCodeAndAnalysisTypeCodeAndActive(
							TEMPLATE_CATEGORY_ANALYSIS_SUMMARY,
							keyword,
							true
					)
					.or(() ->
							meetingTemplateRepository.findByTemplateCategoryCodeAndTemplateNameAndActive(
									TEMPLATE_CATEGORY_ANALYSIS_SUMMARY,
									keyword,
									true
							)
					)
					.orElseThrow(() -> new IllegalStateException("템플릿 관리 화면에서 분석 템플릿을 등록해주세요. analysisType=" + keyword));
		}

		return meetingTemplateRepository
				.findByTemplateCategoryCodeAndActiveOrderBySortOrderAscTemplateNameAsc(
						TEMPLATE_CATEGORY_ANALYSIS_SUMMARY,
						true
				)
				.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("템플릿 관리 화면에서 분석 템플릿을 등록해주세요."));
	}

	// 5-6. 모든 분석유형에서 공통으로 먼저 수행하는 화자분리 템플릿을 찾는다.
	private MeetingTemplate resolveSpeakerSeparationTemplate() {
		return meetingTemplateRepository
				.findByAnalysisTypeCodeAndActive(TEMPLATE_CODE_SPEAKER_SEPARATION, true)
				.orElseThrow(() -> new IllegalStateException("템플릿 관리 화면에서 화자분리 템플릿을 등록해주세요."));
	}

	// 5-7. DB 프롬프트 템플릿의 치환값을 실제 회의 제목, 분석유형, 전사문으로 바꾼다.
	private String renderPromptTemplate(
			String templateText,
			String title,
			String analysisTypeName,
			String transcript
	) {
		String prompt = templateText == null || templateText.isBlank()
				? ""
				: templateText;

		return prompt
				.replace("{title}", title == null ? "" : title)
				.replace("{analysisType}", analysisTypeName == null ? "" : analysisTypeName)
				.replace("{transcript}", transcript == null ? "" : transcript);
	}

}
