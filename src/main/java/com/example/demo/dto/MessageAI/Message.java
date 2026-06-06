package com.example.demo.dto.MessageAI;

import java.util.List;
import lombok.Data;

@Data
public class Message {
	private String role;
	private String content;
	private Object refusal;
	private List<Object> annotations;
}
