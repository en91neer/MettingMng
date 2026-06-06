package com.example.demo.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendLoginCode(String toEmail, String code) {
        JavaMailSender mailSender =
                mailSenderProvider
                        .getIfAvailable();

        if (mailSender == null) {
            throw new RuntimeException("메일 발송 설정이 없습니다.");
        }

        SimpleMailMessage message = new SimpleMailMessage();

        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }

        message.setTo(toEmail);
        message.setSubject("[AI 음성 회의록 분석] 로그인 인증코드");
        message.setText("""
                AI 음성 회의록 분석 로그인 인증코드입니다.

                인증코드: %s

                인증코드는 10분 동안만 사용할 수 있습니다.
                """.formatted(code));

        mailSender.send(message);
    }
}
