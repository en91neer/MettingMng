package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMinutesRequestDto {
    private String subject;
    private String subjectCode;
    private String content;
    private String meetingDate;
    private String startDate;
    private String endDate;
    private String analysisType;
    private String loginEmail;
    private Integer offset;
    private Integer limit;
}
