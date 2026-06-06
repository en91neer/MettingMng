package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImplementationInquirySaveRequestDto {
    private String customerName;
    private String email;
    private String phoneNumber;
    private String inquiryContent;
}
