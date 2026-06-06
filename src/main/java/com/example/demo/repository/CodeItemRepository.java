package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.CodeItem;

public interface CodeItemRepository extends JpaRepository<CodeItem, Long> {
    List<CodeItem> findByGroupCodeAndActiveOrderBySortOrderAscCodeNameAsc(String groupCode, Boolean active);

    List<CodeItem> findByGroupCodeOrderBySortOrderAscCodeNameAsc(String groupCode);

    List<CodeItem> findAllByOrderByGroupCodeAscSortOrderAscCodeNameAsc();

    Optional<CodeItem> findByGroupCodeAndCode(String groupCode, String code);

    void deleteByGroupCode(String groupCode);
}
