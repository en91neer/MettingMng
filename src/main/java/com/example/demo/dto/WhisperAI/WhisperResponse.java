package com.example.demo.dto.WhisperAI;

import lombok.Data;

@Data
public class WhisperResponse {
	private String text;
	private Usage usage;
}
