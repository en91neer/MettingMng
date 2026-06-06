package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.ImplementationInquiry;

import lombok.Getter;

@Getter
public class ImplementationInquiryDto {
    private final Long id;
    private final String customerName;
    private final String email;
    private final String phoneNumber;
    private final String inquiryContent;
    private final LocalDateTime createdAt;

    public ImplementationInquiryDto(ImplementationInquiry inquiry) {
        this.id = inquiry.getId();
        this.customerName = inquiry.getCustomerName();
        this.email = inquiry.getEmail();
        this.phoneNumber = inquiry.getPhoneNumber();
        this.inquiryContent = inquiry.getInquiryContent();
        this.createdAt = inquiry.getCreatedAt();
    }
}
