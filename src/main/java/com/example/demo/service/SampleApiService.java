package com.example.demo.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.SampleApiDto;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class SampleApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<SampleApiDto> sampleApi() {

    	log.info("sampleApi 진입");
    	
        String url = "https://jsonplaceholder.typicode.com/posts";

        ResponseEntity<SampleApiDto[]> response =
                restTemplate.getForEntity(url, SampleApiDto[].class);
        
        log.info("sampleApi 호출됨 {}", response);

        return Arrays.asList(response.getBody());
    }
}