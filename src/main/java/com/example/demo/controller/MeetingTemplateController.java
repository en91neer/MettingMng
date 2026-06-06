package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MeetingTemplateDto;
import com.example.demo.dto.MeetingTemplateSaveRequestDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.MeetingTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class MeetingTemplateController {

    private final MeetingTemplateService meetingTemplateService;
    private final AuthService authService;

    @GetMapping("/active")
    public List<MeetingTemplateDto> activeTemplates(
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail
    ) {
        return meetingTemplateService.getActiveTemplates(loginEmail);
    }

    @GetMapping
    public List<MeetingTemplateDto> templates(
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return meetingTemplateService.getAllTemplates();
    }

    @PostMapping
    public MeetingTemplateDto save(
            @RequestBody MeetingTemplateSaveRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return meetingTemplateService.save(dto);
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);
        meetingTemplateService.delete(id);

        return "삭제완료";
    }
}
