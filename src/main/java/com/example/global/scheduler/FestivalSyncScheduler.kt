package com.example.global.scheduler

import com.example.domain.festival.service.FestivalSyncService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 축제 동기화 스케줄러
@Component
class FestivalSyncScheduler(
    private val festivalSyncService: FestivalSyncService
) {
    @Scheduled(cron = "0 0 0 * * *")
    fun syncFestivalData() {
        val eventStartDate =
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)

        log.info(
            "[FestivalScheduler] 스케줄러 실행 요청 - eventStartDate={}",
            eventStartDate
        )

        festivalSyncService.runScheduledSync(eventStartDate, 1, 200)
    }

    companion object {
        private val log = LoggerFactory.getLogger(FestivalSyncScheduler::class.java)
    }
}