package com.example.global.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    private final RestTemplate restTemplate;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public void sendMessage(String message) {
        if (!StringUtils.hasText(webhookUrl)) {
            System.out.println("[Slack] webhook url이 설정되지 않아 알림을 생략합니다.");
            return;
        }

        Map<String, String> request = Map.of("text", message);

        try {
            restTemplate.postForEntity(webhookUrl, request, String.class);
            System.out.println("[Slack] 알림 전송 완료");
        } catch (Exception e) {
            System.out.println("[Slack] 알림 전송 실패: " + e.getMessage());
        }
    }
}