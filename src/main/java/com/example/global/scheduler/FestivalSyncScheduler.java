package com.example.global.scheduler;

import com.example.domain.festival.service.FestivalSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

//@Slf4j 추후 로그 도입 시 적용할 것.
@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

    private final FestivalSyncService festivalSyncService;

    @Scheduled(cron = "0 0 0 * * *")
    public void syncFestivalData() {

        String eventStartDate =
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        try {
            log.info("[FestivalScheduler] 스케줄러 실행 시작 - eventStartDate={}",
                    eventStartDate);

            festivalSyncService.runScheduledSync(eventStartDate, 1, 200);

            log.info("[FestivalScheduler] 스케줄러 실행 완료 - eventStartDate={}",
                    eventStartDate);

        } catch (Exception e) {
            log.error("[FestivalScheduler] 스케줄러 실행 실패 - eventStartDate={}, message={}",
                    eventStartDate,
                    e.getMessage(),
                    e);
        }
    }
}