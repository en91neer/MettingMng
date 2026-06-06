package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ImplementationInquiry;

public interface ImplementationInquiryRepository extends JpaRepository<ImplementationInquiry, Long> {
    List<ImplementationInquiry> findAllByOrderByCreatedAtDesc();
}
