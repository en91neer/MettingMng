package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.MeetingMinutesRequestDto;
import com.example.demo.dto.AnalysisResultDto;
import com.example.demo.dto.AnalysisResultUpdateRequestDto;
import com.example.demo.dto.MeetingSubjectUpdateRequestDto;
import com.example.demo.dto.TranscriptUpdateRequestDto;
import com.example.demo.entity.AnalysisResult;
import com.example.demo.entity.FileAttach;
import com.example.demo.entity.MeetingMinutes;
import com.example.demo.entity.MeetingMinutesResult;
import com.example.demo.entity.UserActionLog;
import com.example.demo.repository.AnalysisResultRepository;
import com.example.demo.repository.FileAttachRepository;
import com.example.demo.repository.MeetingMinutesRepository;
import com.example.demo.repository.UserActionLogRepository;
import com.example.demo.repository.mapper.MeetingMinutesMapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingMinutesService {

    private final MeetingMinutesRepository meetingMinutesRepository;
    private final FileAttachRepository fileAttachRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserActionLogRepository userActionLogRepository;
    private final MeetingMinutesMapper meetingMinutesMapper;
    private final AuthService authService;
    
    @Value("${file.upload-root-path:c:/dog-walk-nas/upload/}")
    private String rootPath;
    
    //회의현황 리스트 조회
    public List<MeetingMinutesResult> meetingMinutesList(MeetingMinutesRequestDto dto) {

        List<MeetingMinutesResult> list = meetingMinutesMapper.meetingMinutesList(dto);

        list.forEach(item -> {
            item.setTranscript(
                    readTextFile(
                            item.getTranscriptFilePath()
                    )
            );
            item.setSummary(
                    readTextFile(
                            item.getSummaryFilePath()
                    )
            );
    	});
    	
        return list;
    }
    
    //파일경로 조회
    public String getImgPath(Long fileId, String loginEmail) {
    	FileAttach fileAttach = fileAttachRepository
                        .findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "파일 없음"
                                ));
        validateFileOwner(fileAttach, loginEmail);

        return fileAttach.getAudioFilePath();
    }

    @Transactional
    public void deleteMeetingMinutes(Long meetingMinutesId, String loginEmail) {
        log.info(">>>회의정보 삭제 meetingMinutesId={}, loginEmail={}", meetingMinutesId, loginEmail);

        if (
                !authService.isSuperUserEmail(loginEmail)
                        && !meetingMinutesRepository.existsByIdAndMeetingOwnerId(meetingMinutesId, loginEmail)
        ) {
            throw new RuntimeException("삭제할 회의정보가 없습니다.");
        }

        analysisResultRepository.deleteByMeetingMinutesId(meetingMinutesId);
        fileAttachRepository.deleteByTargetId(meetingMinutesId);
        meetingMinutesRepository.deleteById(meetingMinutesId);
        saveUserActionLog(loginEmail, "DELETE", "MEETING_MINUTES", meetingMinutesId);
    }

    public List<AnalysisResultDto> getAnalysisResults(Long fileId, String loginEmail) {
        FileAttach ownerFileAttach =
                fileAttachRepository
                        .findById(fileId)
                        .orElseThrow(() -> new RuntimeException("파일 정보가 없습니다."));
        validateFileOwner(ownerFileAttach, loginEmail);

        List<AnalysisResultDto> results =
                analysisResultRepository
                        .findByFileIdOrderByAnalyzedAtAsc(fileId)
                        .stream()
                        .map(AnalysisResultDto::new)
                        .collect(Collectors.toList());

        if (!results.isEmpty()) {
            return results;
        }

        return fileAttachRepository
                .findById(fileId)
                .filter(fileAttach -> fileAttach.getSummaryFilePath() != null)
                .map(fileAttach -> List.of(
                        new AnalysisResultDto(
                                null,
                                fileAttach.getId(),
                                "보고서",
                                "REPORT",
                                readTextFile(fileAttach.getSummaryFilePath()),
                                fileAttach.getUpdatedAt()
                        )
                ))
                .orElseGet(List::of);
    }

    @Transactional
    public AnalysisResultDto updateAnalysisResult(AnalysisResultUpdateRequestDto dto) {
        FileAttach fileAttach =
                fileAttachRepository
                        .findById(dto.getFileId())
                        .orElseThrow(() -> new RuntimeException("파일 정보가 없습니다."));
        validateFileOwner(fileAttach, dto.getLoginEmail());

        AnalysisResult analysisResult =
                analysisResultRepository
                        .findByFileIdAndAnalysisTypeCode(
                                dto.getFileId(),
                                resolveAnalysisTypeCode(dto.getAnalysisTypeCode(), dto.getAnalysisType())
                        )
                        .orElseGet(() ->
                                AnalysisResult.builder()
                                        .meetingMinutesId(fileAttach.getTargetId())
                                        .fileId(fileAttach.getId())
                                        .analysisTypeCode(resolveAnalysisTypeCode(dto.getAnalysisTypeCode(), dto.getAnalysisType()))
                                        .build()
                        );

        analysisResult.setContent(dto.getContent());
        analysisResult.setUpdatedBy(dto.getLoginEmail());

        if (analysisResult.getCreatedBy() == null) {
            analysisResult.setCreatedBy(dto.getLoginEmail());
        }

        AnalysisResult savedAnalysisResult = analysisResultRepository.save(analysisResult);
        saveUserActionLog(
                dto.getLoginEmail(),
                "UPDATE",
                "ANALYSIS_RESULT",
                savedAnalysisResult.getId()
        );

        return new AnalysisResultDto(savedAnalysisResult);
    }

    @Transactional
    public void updateSubject(MeetingSubjectUpdateRequestDto dto) {
        if (dto.getSubject() == null || dto.getSubject().isBlank()) {
            throw new RuntimeException("회의 주제를 입력해주세요.");
        }

        MeetingMinutes meetingMinutes =
                meetingMinutesRepository
                        .findById(dto.getId())
                        .orElseThrow(() -> new RuntimeException("회의정보가 없습니다."));

        if (
                !authService.isSuperUserEmail(dto.getLoginEmail())
                        && !dto.getLoginEmail().equals(meetingMinutes.getMeetingOwnerId())
        ) {
            throw new RuntimeException("본인 회의정보만 수정할 수 있습니다.");
        }

        meetingMinutes.setSubject(dto.getSubject().trim());
        meetingMinutes.setUpdatedBy(dto.getLoginEmail());
        meetingMinutesRepository.save(meetingMinutes);
        saveUserActionLog(
                dto.getLoginEmail(),
                "UPDATE",
                "MEETING_MINUTES_SUBJECT",
                meetingMinutes.getId()
        );
    }

    @Transactional
    public void updateTranscript(TranscriptUpdateRequestDto dto) throws IOException {
        FileAttach fileAttach =
                fileAttachRepository
                        .findById(dto.getFileId())
                        .orElseThrow(() -> new RuntimeException("파일 정보가 없습니다."));
        validateFileOwner(fileAttach, dto.getLoginEmail());

        if (
                fileAttach.getTranscriptFilePath() == null
                || fileAttach.getTranscriptFilePath().isBlank()
        ) {
            throw new RuntimeException("전사문 파일이 없습니다.");
        }

        Path transcriptPath = Paths.get(fileAttach.getTranscriptFilePath());
        if (!Files.exists(transcriptPath)) {
            throw new RuntimeException("전사문 파일을 찾을 수 없습니다.");
        }

        Files.writeString(
                transcriptPath,
                dto.getContent() == null ? "" : dto.getContent(),
                StandardCharsets.UTF_8
        );
        fileAttach.setUpdatedBy(dto.getLoginEmail());
        fileAttachRepository.save(fileAttach);
        saveUserActionLog(
                dto.getLoginEmail(),
                "UPDATE",
                "TRANSCRIPT",
                fileAttach.getId()
        );
    }
    
    // 파일정보 저장
	public void save(MeetingMinutesRequestDto dto, List<MultipartFile> files, String loginEmail) throws IllegalStateException, IOException
	{
		log.info(">>>회의록 정보 저장");
        
		LocalDateTime tempDateTime = LocalDateTime.parse(dto.getMeetingDate() + ":00",
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
        MeetingMinutes meetingMinutes = MeetingMinutes.builder()
                        .subject(dto.getSubject())
                        .subjectCode(dto.getSubjectCode())
                        .content(dto.getContent())
                        .meetingDate(tempDateTime)
                        .meetingOwnerId(loginEmail)
                        .createdAt(LocalDateTime.now())
                        .createdBy(loginEmail)
                        .updatedBy(loginEmail)
                        .build();
		
        MeetingMinutes savedMeetingMinutes = meetingMinutesRepository.save(meetingMinutes);
        saveUserActionLog(loginEmail, "CREATE", "MEETING_MINUTES", savedMeetingMinutes.getId());

        // 파일 저장
        if (files != null) {
        	for (MultipartFile file : files) {
        		
        		log.info(">>>첨부파일 있음");

        	    // 저장 파일명 생성
                String savedName =
                        UUID.randomUUID() + "_"
                        + sanitizeFileName(file.getOriginalFilename());

                // 전체 파일 경로
                Path uploadPath = getUploadDirectory(loginEmail);
                Files.createDirectories(uploadPath);
                String fullPath =
                        uploadPath
                                .resolve(savedName)
                                .toString();

        	    log.info(">>>첨부파일 {}", fullPath);

        	    // 실제 파일 저장
        	    file.transferTo(new File(fullPath));

        	    // DB 저장
                FileAttach fileAttach =
                        FileAttach.builder()
                                .targetTypeCode("MEETING_MINUTES_AUDIO")
                                .targetId(savedMeetingMinutes.getId())
                                .originalName(file.getOriginalFilename())
                                .savedName(savedName)
                                .audioFilePath(fullPath)
                                .createdAt(LocalDateTime.now())
                                .createdBy(loginEmail)
                                .updatedBy(loginEmail)
                                .build();

        	    fileAttachRepository.save(fileAttach);
        	    
        	    log.info(">>>첨부파일 저장완료");
        	}
        }
    }
	
	public void excelDownload(MeetingMinutesRequestDto dto, HttpServletResponse response) throws Exception {

        dto.setOffset(null);
        dto.setLimit(null);

        List<MeetingMinutesResult> list = meetingMinutesMapper.meetingMinutesList(dto);
        Map<Long, List<AnalysisResult>> analysisResultsByFileId =
                getExcelAnalysisResultsByFileId(list, dto.getAnalysisType());

        // workbook 생성
        XSSFWorkbook workbook = new XSSFWorkbook();
        CellStyle wrapTextStyle = workbook.createCellStyle();
        wrapTextStyle.setWrapText(true);

        Sheet sheet = workbook.createSheet("회의분석목록");

        int rowNo = 0;

        // 헤더
        Row headerRow = sheet.createRow(rowNo++);

        headerRow.createCell(0).setCellValue("번호");
        headerRow.createCell(1).setCellValue("로그인ID");
        headerRow.createCell(2).setCellValue("회의주제");
        headerRow.createCell(3).setCellValue("회의요약");
        headerRow.createCell(4).setCellValue("회의날짜");
        headerRow.createCell(5).setCellValue("등록날짜");
        headerRow.createCell(6).setCellValue("파일ID");
        headerRow.createCell(7).setCellValue("물리파일위치");
        headerRow.createCell(8).setCellValue("전사파일위치");
        headerRow.createCell(9).setCellValue("요약파일위치");
        headerRow.createCell(10).setCellValue("분석상태");
        headerRow.createCell(11).setCellValue("분석유형");
        headerRow.createCell(12).setCellValue("분석내용");
        headerRow.createCell(13).setCellValue("분석일시");

        // 데이터
        for (MeetingMinutesResult item : list) {
            Long fileId = parseLong(item.getFileId());
            List<AnalysisResult> analysisResults =
                    fileId == null
                            ? List.of()
                            : analysisResultsByFileId.getOrDefault(fileId, List.of());

            if (analysisResults.isEmpty()) {
                Row row = sheet.createRow(rowNo++);
                writeExcelRow(row, item, null, wrapTextStyle);
                continue;
            }

            for (AnalysisResult analysisResult : analysisResults) {
                Row row = sheet.createRow(rowNo++);
                writeExcelRow(row, item, analysisResult, wrapTextStyle);
            }
        }

        // 컬럼 자동 크기
        for (int i = 0; i < 14; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.setColumnWidth(7, 12000);
        sheet.setColumnWidth(8, 12000);
        sheet.setColumnWidth(9, 12000);
        sheet.setColumnWidth(12, 20000);

        String fileName = URLEncoder.encode("회의분석목록.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        response.setHeader("Content-Disposition","attachment; filename=" + fileName);

        ServletOutputStream outputStream = response.getOutputStream();

        workbook.write(outputStream);
        workbook.close();

        outputStream.close();
    }

    private Map<Long, List<AnalysisResult>> getExcelAnalysisResultsByFileId(
            List<MeetingMinutesResult> list,
            String analysisType
    ) {
        List<Long> fileIds =
                list.stream()
                        .map(MeetingMinutesResult::getFileId)
                        .map(this::parseLong)
                        .filter(fileId -> fileId != null && fileId > 0)
                        .distinct()
                        .collect(Collectors.toList());

        if (fileIds.isEmpty()) {
            return Map.of();
        }

        return analysisResultRepository
                .findByFileIdInOrderByFileIdAscAnalyzedAtAsc(fileIds)
                .stream()
                .filter(result -> isExcelAnalysisTypeMatched(result, analysisType))
                .sorted(
                        Comparator
                                .comparing(AnalysisResult::getFileId)
                                .thenComparing(
                                        AnalysisResult::getAnalyzedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                )
                )
                .collect(Collectors.groupingBy(AnalysisResult::getFileId));
    }

    private boolean isExcelAnalysisTypeMatched(AnalysisResult result, String analysisType) {
        String analysisTypeCode = resolveAnalysisTypeCode(analysisType, analysisType);

        return (
                analysisType == null
                || analysisType.isBlank()
                || "전체".equals(analysisType)
                || analysisTypeCode.equals(result.getAnalysisTypeCode())
        );
    }

    private String resolveAnalysisTypeCode(String analysisTypeCode, String analysisType) {
        if (analysisTypeCode != null && !analysisTypeCode.isBlank()) {
            return analysisTypeCode.trim();
        }

        if (analysisType == null || analysisType.isBlank()) {
            return "GENERAL_MEETING";
        }

        return switch (analysisType.trim()) {
            case "개발회의" -> "DEV_MEETING";
            case "상담회의" -> "CONSULTING_MEETING";
            case "일반회의" -> "GENERAL_MEETING";
            default -> analysisType.trim();
        };
    }

    private void writeExcelRow(
            Row row,
            MeetingMinutesResult item,
            AnalysisResult analysisResult,
            CellStyle wrapTextStyle
    ) {
        row.createCell(0).setCellValue(item.getId());
        row.createCell(1).setCellValue(item.getMeetingOwnerId());
        row.createCell(2).setCellValue(item.getSubject());
        row.createCell(3).setCellValue(item.getContent());
        row.createCell(4).setCellValue(item.getMeetingDate());
        row.createCell(5).setCellValue(item.getCreatedAt());
        row.createCell(6).setCellValue(item.getFileId());
        row.createCell(7).setCellValue(item.getAudioFilePath());
        row.createCell(8).setCellValue(item.getTranscriptFilePath());
        row.createCell(9).setCellValue(item.getSummaryFilePath());
        row.createCell(10).setCellValue(item.getAnalyzeStatus());
        row.createCell(11).setCellValue(
                analysisResult == null
                        ? ""
                        : analysisResult.getAnalysisTypeCode()
        );
        row.createCell(12).setCellValue(
                analysisResult == null
                        ? ""
                        : analysisResult.getContent()
        );
        row.getCell(12).setCellStyle(wrapTextStyle);
        row.createCell(13).setCellValue(
                analysisResult == null || analysisResult.getAnalyzedAt() == null
                        ? ""
                        : analysisResult.getAnalyzedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    private Long parseLong(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }

            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Path getUploadDirectory(String loginEmail) {
        LocalDate now = LocalDate.now();

        return Paths.get(
                rootPath,
                sanitizePathSegment(loginEmail),
                String.valueOf(now.getYear()),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth())
        );
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value.replaceAll("[\\\\/:*?\"<>|]", "_").strip();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload";
        }

        return sanitizePathSegment(Paths.get(fileName).getFileName().toString());
    }
	
	//해당경로의 파일을 읽는다.
    private String readTextFile(String filePath) {
		try {
			if (filePath == null) {
				return null;
			}
			Path path = Paths.get(filePath);
			if (!Files.exists(path)) {
				return null;
			}
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.error("파일 읽기 실패 : {}", filePath, e);

			return null;
		}
	}

    private void saveUserActionLog(
            String loginEmail,
            String actionType,
            String targetType,
            Long targetId
    ) {
        UserActionLog log = new UserActionLog();
        log.setLoginEmail(loginEmail);
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);

        userActionLogRepository.save(log);
    }

    private void validateFileOwner(FileAttach fileAttach, String loginEmail) {
        if (loginEmail == null || loginEmail.isBlank()) {
            throw new RuntimeException("로그인 정보가 없습니다.");
        }

        if (
                !authService.isSuperUserEmail(loginEmail)
                        && !loginEmail.equals(fileAttach.getCreatedBy())
        ) {
            throw new RuntimeException("본인 파일만 조회할 수 있습니다.");
        }
    }
}
