package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SampleApiDto;
import com.example.demo.service.SampleApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sample")
@CrossOrigin(origins = "*")
@Slf4j
public class SampleApiController {

	private final SampleApiService sampleApiService;

    /**
	 * 서버 투 서버통신 확인
	 */
    @PostMapping("/sampleApi")
    public List<SampleApiDto> sampleApi() {
        return sampleApiService.sampleApi();
    }
}
