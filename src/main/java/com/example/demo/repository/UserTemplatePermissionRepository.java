package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.UserTemplatePermission;

public interface UserTemplatePermissionRepository extends JpaRepository<UserTemplatePermission, Long> {
    List<UserTemplatePermission> findByUserEmailIgnoreCase(String userEmail);

    void deleteByUserEmailIgnoreCase(String userEmail);

    void deleteByAnalysisTypeCode(String analysisTypeCode);
}
