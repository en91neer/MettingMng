package com.example.demo.websocket;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebSocketStartupConfig {

    @Bean
    CommandLineRunner startWebSocket(StatsWebSocketHandler handler) {
        return args -> {
        	
        	System.out.println("println 확인");
        	log.error("error 확인");
        	log.warn("warn 확인");
        	log.info("info 확인");
        	
        	log.info(">>>WebSocket Push 시작\"");
            //handler.startPushLoop();
            log.info(">>>WebSocket Push 등록완료\"");
        };
    }
}
