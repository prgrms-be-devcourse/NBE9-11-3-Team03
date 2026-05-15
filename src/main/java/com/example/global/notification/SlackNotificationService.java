package com.example.global.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    private final RestTemplate restTemplate;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public void sendMessage(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[Slack] webhook URL 미설정 → 전송 건너뜀");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity =
                    new HttpEntity<>(Map.of("text", message), headers);

            restTemplate.postForEntity(
                    webhookUrl,
                    entity,
                    String.class
            );

            log.info("[Slack] 알림 전송 완료");

        } catch (Exception e) {
            log.error("[Slack] 알림 전송 실패 - message={}",
                    e.getMessage(),
                    e);
        }
    }
}