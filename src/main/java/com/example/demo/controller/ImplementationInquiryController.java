package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ImplementationInquiryDto;
import com.example.demo.dto.ImplementationInquirySaveRequestDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.ImplementationInquiryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/implementation-inquiries")
@RequiredArgsConstructor
public class ImplementationInquiryController {

    private final ImplementationInquiryService implementationInquiryService;
    private final AuthService authService;

    @PostMapping
    public ImplementationInquiryDto save(@RequestBody ImplementationInquirySaveRequestDto dto) {
        return implementationInquiryService.save(dto);
    }

    @GetMapping
    public List<ImplementationInquiryDto> inquiries(
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return implementationInquiryService.getInquiries();
    }
}
