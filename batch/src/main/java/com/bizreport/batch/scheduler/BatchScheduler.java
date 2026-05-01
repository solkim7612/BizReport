package com.bizreport.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {
    private final BatchService service;

    // TODO: Slack API 연동예정

    @Scheduled(cron = "0 0 3 11 1,4,7,10 ?")
    @SchedulerLock(name = "runUpdateLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runUpdate() {
        try {
            service.runUpdate();
        } catch (Exception e) {
            log.error("[BATCH ERROR] 분기별 국세청 동기화 실패", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "runClosedLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runClosed() {
        try {
            service.runClosed();
        } catch (Exception e) {
            log.error("[BATCH ERROR] 폐업일 경과 사업자 상태 자동 전환 실패", e);
        }
    }

    @Scheduled(cron = "0 0 3 16 * ?")
    @SchedulerLock(name = "runReportLock", lockAtLeastFor = "1m", lockAtMostFor = "2h")
    public void runReport() {
        try {
            service.runReport();
        } catch (Exception e) {
            log.error("[BATCH ERROR] 월간 리포트 자동 생성 실패", e);
        }
    }
}