package com.example.demo.dto.WhisperAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class InputTokenDetails {
	@JsonProperty("text_tokens")
	private int textTokens;

	@JsonProperty("audio_tokens")
	private int audioTokens;
}
