package com.example.demo.websocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StatsWebSocketHandler extends TextWebSocketHandler {

    public final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("웹소켓 연결됨 sessionId={}, sessions={}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("웹소켓 종료됨 sessionId={}, sessions={}", session.getId(), sessions.size());
    }

    public void sendAnalyzeProgress(Long fileId, int progress, String status, String message) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "ANALYZE_PROGRESS",
                    "fileId", fileId,
                    "progress", clamp(progress),
                    "status", status,
                    "message", message
            ));

            log.info("분석 진행률 전송 fileId={}, progress={}%, status={}, sessions={}",
                    fileId, progress, status, sessions.size());

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
            }
        } catch (Exception e) {
            log.error("분석 진행률 전송 실패 fileId={}", fileId, e);
        }
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
