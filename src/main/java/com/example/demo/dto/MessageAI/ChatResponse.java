package com.example.demo.dto.MessageAI;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatResponse {
	 private String id;
	    private String object;
	    private Long created;
	    private String model;

	    private List<Choice> choices;

	    private Usage usage;

	    @JsonProperty("service_tier")
	    private String serviceTier;

	    @JsonProperty("system_fingerprint")
	    private String systemFingerprint;
}
