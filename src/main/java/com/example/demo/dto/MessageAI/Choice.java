package com.example.demo.dto.MessageAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Choice {
    private Integer index;
    private Message message;
    private Object logprobs;
    
    @JsonProperty("finish_reason")
    private String finishReason;
}
