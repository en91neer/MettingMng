package com.example.demo.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.CodeGroup;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, Long> {
    Optional<CodeGroup> findByGroupCode(String groupCode);

    List<CodeGroup> findAllByOrderByGroupCodeAsc();
}
