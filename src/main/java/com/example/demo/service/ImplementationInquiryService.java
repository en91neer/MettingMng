package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ImplementationInquiryDto;
import com.example.demo.dto.ImplementationInquirySaveRequestDto;
import com.example.demo.entity.ImplementationInquiry;
import com.example.demo.repository.ImplementationInquiryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImplementationInquiryService {

    private final ImplementationInquiryRepository implementationInquiryRepository;

    @Transactional
    public ImplementationInquiryDto save(ImplementationInquirySaveRequestDto dto) {
        ImplementationInquiry inquiry = new ImplementationInquiry();
        inquiry.setCustomerName(require(dto.getCustomerName(), "성함을 입력해주세요."));
        inquiry.setEmail(require(dto.getEmail(), "이메일을 입력해주세요."));
        inquiry.setPhoneNumber(require(dto.getPhoneNumber(), "전화번호를 입력해주세요."));
        inquiry.setInquiryContent(require(dto.getInquiryContent(), "문의내용을 입력해주세요."));

        return new ImplementationInquiryDto(implementationInquiryRepository.save(inquiry));
    }

    public List<ImplementationInquiryDto> getInquiries() {
        return implementationInquiryRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ImplementationInquiryDto::new)
                .toList();
    }

    @Transactional
    public void deleteInquiry(Long inquiryId) {
        if (!implementationInquiryRepository.existsById(inquiryId)) {
            throw new RuntimeException("도입문의를 찾을 수 없습니다.");
        }

        implementationInquiryRepository.deleteById(inquiryId);
    }

    private String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }

        return value.trim();
    }
}
