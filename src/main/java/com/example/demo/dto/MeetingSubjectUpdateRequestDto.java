package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingSubjectUpdateRequestDto {
    private Long id;
    private String subject;
    private String loginEmail;
}
