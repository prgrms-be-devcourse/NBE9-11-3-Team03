package com.example.global.scheduler;

import com.example.domain.festival.service.FestivalSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

//@Slf4j 추후 로그 도입 시 적용할 것.
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

    private final FestivalSyncService festivalSyncService;

    //@Scheduled(cron = "0 0 0 * * *") //시작할 시간
    public void syncFestivalData() {
        String eventStartDate =
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        System.out.println("[FestivalScheduler] 스케줄러 실행 시작");

        festivalSyncService.runScheduledSync(eventStartDate, 1, 200);

        System.out.println("[FestivalScheduler] 스케줄러 실행 완료");
    }
}