package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CodeGroupDto;
import com.example.demo.dto.CodeGroupSaveRequestDto;
import com.example.demo.dto.CodeItemDto;
import com.example.demo.dto.CodeItemSaveRequestDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.CodeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;
    private final AuthService authService;

    @GetMapping("/groups")
    public List<CodeGroupDto> groups(
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return codeService.getGroups();
    }

    @GetMapping("/{groupCode}")
    public List<CodeItemDto> codes(@PathVariable String groupCode) {
        return codeService.getActiveCodes(groupCode);
    }

    @GetMapping("/{groupCode}/all")
    public List<CodeItemDto> allCodes(
            @PathVariable String groupCode,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return codeService.getAllCodes(groupCode);
    }

    @PostMapping("/groups")
    public CodeGroupDto saveGroup(
            @RequestBody CodeGroupSaveRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return codeService.saveGroup(dto);
    }

    @PostMapping("/items")
    public CodeItemDto saveItem(
            @RequestBody CodeItemSaveRequestDto dto,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);

        return codeService.saveItem(dto);
    }

    @DeleteMapping("/groups/{id}")
    public String deleteGroup(
            @PathVariable Long id,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);
        codeService.deleteGroup(id);

        return "삭제 완료";
    }

    @DeleteMapping("/items/{id}")
    public String deleteItem(
            @PathVariable Long id,
            @RequestHeader(value = "X-Login-Email", required = false) String loginEmail,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken
    ) {
        authService.validateAdmin(loginEmail, authToken);
        codeService.deleteItem(id);

        return "삭제 완료";
    }
}
