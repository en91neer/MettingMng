package com.example.demo.dto.WhisperAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Usage {
	private String type;

	@JsonProperty("total_tokens")
	private int totalTokens;

	@JsonProperty("input_tokens")
	private int inputTokens;

	@JsonProperty("output_tokens")
	private int outputTokens;

	@JsonProperty("input_token_details")
	private InputTokenDetails inputTokenDetails;

}
