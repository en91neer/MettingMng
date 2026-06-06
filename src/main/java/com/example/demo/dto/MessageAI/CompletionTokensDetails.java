package com.example.demo.dto.MessageAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CompletionTokensDetails {

    @JsonProperty("reasoning_tokens")
    private Integer reasoningTokens;

    @JsonProperty("audio_tokens")
    private Integer audioTokens;

    @JsonProperty("accepted_prediction_tokens")
    private Integer acceptedPredictionTokens;

    @JsonProperty("rejected_prediction_tokens")
    private Integer rejectedPredictionTokens;
}
