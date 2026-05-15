package com.example.global.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SlackNotificationService(
    private val restTemplate: RestTemplate,
    @Value("\${slack.webhook.url:}")
    private val webhookUrl: String
) {
    fun sendMessage(message: String) {
        if (webhookUrl.isBlank()) {
            log.debug("[Slack] webhook URL 미설정 → 전송 건너뜀")
            return
        }

        try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val entity = HttpEntity(mapOf("text" to message), headers)

            restTemplate.postForEntity(
                webhookUrl,
                entity,
                String::class.java
            )

            log.info("[Slack] 알림 전송 완료")
        } catch (e: Exception) {
            log.error("[Slack] 알림 전송 실패 - message=${e.message}", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SlackNotificationService::class.java)
    }
}