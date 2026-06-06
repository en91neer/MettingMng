package com.example.demo.dto.MessageAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PromptTokensDetails {

    @JsonProperty("cached_tokens")
    private Integer cachedTokens;

    @JsonProperty("audio_tokens")
    private Integer audioTokens;
}
