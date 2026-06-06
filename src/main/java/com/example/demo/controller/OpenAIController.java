package com.example.demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AuthService;
import com.example.demo.service.OpenAIService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/metting")
@RequiredArgsConstructor
public class OpenAIController {
	
	private final OpenAIService openAIService;
	private final AuthService authService;

	@GetMapping("/analyze/check")
	public Map<String, Object> checkAnalyzeAllowed(
			@RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
			@RequestHeader(value = "X-Auth-Token", required = false) String authToken
	) {
		authService.validateSession(loginEmail, authToken);

		return authService.getAnalyzeUsage(loginEmail);
	}
	
	/**
	 * 분석버튼 클릭시 분석진행
	 */
	@GetMapping("/analyze")
    public String mettingAnalyze(
    		@RequestParam Long fileId,
    		@RequestParam String title,
            @RequestParam(defaultValue = "GENERAL_MEETING") String analysisType,
    		@RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
    		@RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) throws Exception {
		authService.validateSession(loginEmail, authToken);
		authService.validateAnalyzeAllowed(loginEmail);
		
		//AI분석 진행(비동기 처리)
		openAIService.openAiAnalyze(title, fileId, analysisType, loginEmail);
		
		return "요청성공";
    }
}
