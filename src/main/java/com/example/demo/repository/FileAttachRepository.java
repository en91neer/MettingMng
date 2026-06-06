package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.example.demo.entity.FileAttach;

public interface FileAttachRepository extends JpaRepository<FileAttach, Long> {
    Optional<FileAttach> findByTargetId(Long targetId);

    void deleteByTargetId(Long targetId);
}
