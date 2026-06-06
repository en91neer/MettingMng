package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CodeGroupDto;
import com.example.demo.dto.CodeGroupSaveRequestDto;
import com.example.demo.dto.CodeItemDto;
import com.example.demo.dto.CodeItemSaveRequestDto;
import com.example.demo.entity.CodeGroup;
import com.example.demo.entity.CodeItem;
import com.example.demo.repository.CodeGroupRepository;
import com.example.demo.repository.CodeItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeGroupRepository codeGroupRepository;
    private final CodeItemRepository codeItemRepository;

    public List<CodeGroupDto> getGroups() {
        return codeGroupRepository
                .findAllByOrderByGroupCodeAsc()
                .stream()
                .map(CodeGroupDto::new)
                .toList();
    }

    public List<CodeItemDto> getAllCodes(String groupCode) {
        return codeItemRepository
                .findByGroupCodeOrderBySortOrderAscCodeNameAsc(groupCode)
                .stream()
                .map(CodeItemDto::new)
                .toList();
    }

    public List<CodeItemDto> getActiveCodes(String groupCode) {
        return codeItemRepository
                .findByGroupCodeAndActiveOrderBySortOrderAscCodeNameAsc(groupCode, true)
                .stream()
                .map(CodeItemDto::new)
                .toList();
    }

    @Transactional
    public CodeGroupDto saveGroup(CodeGroupSaveRequestDto dto) {
        CodeGroup group =
                dto.getId() == null
                        ? new CodeGroup()
                        : codeGroupRepository
                                .findById(dto.getId())
                                .orElseThrow(() -> new RuntimeException("코드그룹을 찾을 수 없습니다."));

        if (dto.getId() == null) {
            String groupCode = require(dto.getGroupCode(), "그룹코드를 입력해주세요.");
            codeGroupRepository
                    .findByGroupCode(groupCode)
                    .ifPresent(existing -> {
                        throw new RuntimeException("이미 등록된 그룹코드입니다.");
                    });

            group.setGroupCode(groupCode);
        }

        group.setGroupName(require(dto.getGroupName(), "그룹명을 입력해주세요."));
        group.setDescription(dto.getDescription());
        group.setActive(dto.getActive() == null || dto.getActive());

        return new CodeGroupDto(codeGroupRepository.save(group));
    }

    @Transactional
    public CodeItemDto saveItem(CodeItemSaveRequestDto dto) {
        String groupCode = require(dto.getGroupCode(), "그룹코드를 선택해주세요.");
        String code = require(dto.getCode(), "코드를 입력해주세요.");

        codeGroupRepository
                .findByGroupCode(groupCode)
                .orElseThrow(() -> new RuntimeException("코드그룹을 찾을 수 없습니다."));

        CodeItem item =
                dto.getId() == null
                        ? new CodeItem()
                        : codeItemRepository
                                .findById(dto.getId())
                                .orElseThrow(() -> new RuntimeException("코드를 찾을 수 없습니다."));

        codeItemRepository
                .findByGroupCodeAndCode(groupCode, code)
                .filter(existing -> !existing.getId().equals(dto.getId()))
                .ifPresent(existing -> {
                    throw new RuntimeException("이미 등록된 개별코드입니다.");
                });

        item.setGroupCode(groupCode);
        item.setCode(code);
        item.setCodeName(require(dto.getCodeName(), "코드명을 입력해주세요."));
        item.setDescription(dto.getDescription());
        item.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        item.setActive(dto.getActive() == null || dto.getActive());

        return new CodeItemDto(codeItemRepository.save(item));
    }

    @Transactional
    public void deleteGroup(Long id) {
        CodeGroup group =
                codeGroupRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("코드그룹을 찾을 수 없습니다."));

        codeItemRepository.deleteByGroupCode(group.getGroupCode());
        codeGroupRepository.delete(group);
    }

    @Transactional
    public void deleteItem(Long id) {
        CodeItem item =
                codeItemRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("코드를 찾을 수 없습니다."));

        codeItemRepository.delete(item);
    }

    private String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }

        return value.trim();
    }
}
