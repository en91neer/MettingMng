package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserApprovalRequestDto;
import com.example.demo.dto.UserManagementDto;
import com.example.demo.dto.UserTemplatePermissionSaveRequestDto;
import com.example.demo.entity.User;
import com.example.demo.entity.UserTemplatePermission;
import com.example.demo.repository.MeetingTemplateRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserTemplatePermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String CATEGORY_ANALYSIS_SUMMARY = "ANALYSIS_SUMMARY";

    private final UserRepository userRepository;
    private final MeetingTemplateRepository meetingTemplateRepository;
    private final UserTemplatePermissionRepository userTemplatePermissionRepository;

    public List<UserManagementDto> getUsers() {
        List<String> activeTemplateCodes = getActiveAnalysisTemplateCodeList();
        Set<String> activeTemplateCodeSet = activeTemplateCodes.stream().collect(Collectors.toSet());
        Map<String, List<String>> permissionsByEmail =
                userTemplatePermissionRepository
                        .findAll()
                        .stream()
                        .filter(permission -> activeTemplateCodeSet.contains(permission.getAnalysisTypeCode()))
                        .collect(
                                Collectors.groupingBy(
                                        permission -> permission.getUserEmail().toLowerCase(),
                                        Collectors.mapping(
                                                UserTemplatePermission::getAnalysisTypeCode,
                                                Collectors.toList()
                                        )
                                )
                        );

        return userRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(user ->
                        new UserManagementDto(
                                user,
                                permissionsByEmail.getOrDefault(user.getEmail().toLowerCase(), activeTemplateCodes)
                        )
                )
                .toList();
    }

    @Transactional
    public UserManagementDto approve(UserApprovalRequestDto dto) {
        User user =
                userRepository
                        .findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("가입자를 찾을 수 없습니다."));

        if (dto.getRoleCode() == null || dto.getRoleCode().isBlank()) {
            throw new RuntimeException("권한을 선택해주세요.");
        }

        user.setRoleCode(dto.getRoleCode().trim());
        user.setStatusCode(STATUS_ACTIVE);

        return new UserManagementDto(userRepository.save(user));
    }

    @Transactional
    public void saveTemplatePermissions(UserTemplatePermissionSaveRequestDto dto) {
        if (dto.getUserEmail() == null || dto.getUserEmail().isBlank()) {
            throw new RuntimeException("사용자 이메일이 없습니다.");
        }

        String userEmail = dto.getUserEmail().trim().toLowerCase();
        userTemplatePermissionRepository.deleteByUserEmailIgnoreCase(userEmail);

        if (dto.getAnalysisTypeCodes() == null || dto.getAnalysisTypeCodes().isEmpty()) {
            return;
        }

        Set<String> activeTemplateCodes = getActiveAnalysisTemplateCodes();
        List<UserTemplatePermission> permissions =
                dto.getAnalysisTypeCodes()
                        .stream()
                        .filter(code -> code != null && !code.isBlank())
                        .map(String::trim)
                        .filter(activeTemplateCodes::contains)
                        .distinct()
                        .map(code ->
                                UserTemplatePermission.builder()
                                        .userEmail(userEmail)
                                        .analysisTypeCode(code)
                                        .build()
                        )
                        .toList();

        userTemplatePermissionRepository.saveAll(permissions);
    }

    private Set<String> getActiveAnalysisTemplateCodes() {
        return getActiveAnalysisTemplateCodeList()
                .stream()
                .collect(Collectors.toSet());
    }

    private List<String> getActiveAnalysisTemplateCodeList() {
        return meetingTemplateRepository
                .findByTemplateCategoryCodeAndActiveOrderBySortOrderAscTemplateNameAsc(
                        CATEGORY_ANALYSIS_SUMMARY,
                        true
                )
                .stream()
                .map(template -> template.getAnalysisTypeCode())
                .toList();
    }
}
