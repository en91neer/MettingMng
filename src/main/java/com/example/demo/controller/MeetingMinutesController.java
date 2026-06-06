package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AnalysisResultDto;
import com.example.demo.dto.AnalysisResultUpdateRequestDto;
import com.example.demo.dto.MeetingMinutesRequestDto;
import com.example.demo.dto.TranscriptUpdateRequestDto;
import com.example.demo.entity.MeetingMinutesResult;
import com.example.demo.service.MeetingMinutesService;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/meeting-minutes")
@RequiredArgsConstructor
public class MeetingMinutesController {

    private final MeetingMinutesService meetingMinutesService;
    private final AuthService authService;
    
	/**
	 * 회의록 저장
	 */
	@PostMapping("/save")
    public String saveMeetingMinutes(
            @RequestPart("data") MeetingMinutesRequestDto dto,
            @RequestPart(value = "file", required = false)
            List<MultipartFile> files,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) throws IllegalStateException, IOException {
        validateLogin(loginEmail, authToken);
        meetingMinutesService.save(dto, files, loginEmail);

        return "회의록 저장 완료";
    }
	
	/**
	 * 회의록 파일 업로드 저장
	 */
	@PostMapping("/upload")
    public String uploadMeetingMinutes(
            @RequestPart("data") MeetingMinutesRequestDto dto,
            @RequestPart(value = "file", required = false)
            List<MultipartFile> files,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) throws IllegalStateException, IOException {

        validateLogin(loginEmail, authToken);
        meetingMinutesService.save(dto, files, loginEmail);

        return "분석요청 완료";
    }
	
	/**
	 * 회의록 조회
	 */
    @GetMapping("/list")
    public List<MeetingMinutesResult> meetingMinutesList(
            @ModelAttribute MeetingMinutesRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        validateLogin(loginEmail, authToken);
        dto.setLoginEmail(loginEmail);

        return meetingMinutesService.meetingMinutesList(dto);
    }

    /**
     * 회의정보 삭제
     */
    @DeleteMapping("/delete/{meetingMinutesId}")
    public String deleteMeetingMinutes(
            @PathVariable Long meetingMinutesId,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        validateLogin(loginEmail, authToken);
        meetingMinutesService.deleteMeetingMinutes(meetingMinutesId, loginEmail);

        return "삭제 완료";
    }

    /**
     * 분석결과 상세 조회
     */
    @GetMapping("/analysisResults")
    public List<AnalysisResultDto> analysisResults(
            @RequestParam Long fileId,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        validateLogin(loginEmail, authToken);

        return meetingMinutesService.getAnalysisResults(fileId, loginEmail);
    }

    /**
     * 분석결과 수정
     */
    @PutMapping("/analysisResult")
    public AnalysisResultDto updateAnalysisResult(
            @RequestBody AnalysisResultUpdateRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        validateLogin(loginEmail, authToken);
        dto.setLoginEmail(loginEmail);

        return meetingMinutesService.updateAnalysisResult(dto);
    }

    /**
     * 화자분리 전사문 수정 저장
     */
    @PutMapping("/transcript")
    public String updateTranscript(
            @RequestBody TranscriptUpdateRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) throws IOException {
        validateLogin(loginEmail, authToken);
        dto.setLoginEmail(loginEmail);
        meetingMinutesService.updateTranscript(dto);

        return "수정 완료";
    }
    
    
    /**
	 * 원본이미지 보기
	 */
    @GetMapping("/getDogImg")
    public ResponseEntity<Resource> getDogImg(
            @RequestParam Long fileId,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) throws Exception {
        validateLogin(loginEmail, authToken);
    	
        // 파일 경로 조회
        String filePath = meetingMinutesService.getImgPath(fileId, loginEmail);
        File file = new File(filePath);

        // Resource 생성
        Resource resource = new UrlResource(file.toURI());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
    
    /**
	 * 엑셀다운로드
	 */
    @PostMapping("/excelDownload")
    public void excelDownload(
            @RequestBody MeetingMinutesRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpServletResponse response
    ) throws Exception {
        validateLogin(loginEmail, authToken);
        dto.setLoginEmail(loginEmail);
        meetingMinutesService.excelDownload(dto, response);

    }

    private void validateLogin(String loginEmail, String authToken) {
        authService.validateSession(loginEmail, authToken);
    }
}








