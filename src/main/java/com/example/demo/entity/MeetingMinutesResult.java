package com.example.demo.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingMinutesResult {
	private String id;				//키값
	private String meetingOwnerId;		//로그인ID
	private String subject;			//회의 주제
	private String subjectCode;		//회의 주제 코드
	private String content;			//회의 내용
	private String meetingDate;		//회의날짜
	private String createdAt;		//등록일시
	private String fileId;			//파일ID
	private String audioFilePath;	//원본 물리 파일경로
	private String transcriptFilePath;	//원본추출 파일경로
	private String summaryFilePath;     //분석결과 파일경로
	private String analyzeStatus;       //분석상태
	private String analysisTypeCodes;   //분석유형 목록
	private String analysisHistory;     //분석유형별 분석일시 목록
	private String transcript;	//원본 파일내용
	private String summary;     //분석 내용
}
