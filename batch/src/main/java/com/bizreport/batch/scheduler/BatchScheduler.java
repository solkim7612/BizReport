package com.bizreport.batch.scheduler;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {
    private final BatchService service;

    @Scheduled(fixedDelayString = "60000")
    @SchedulerLock(name = "ConsumerLock", lockAtLeastFor = "10s", lockAtMostFor = "30m")
    public void consumer() {
        service.execute();
    }

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "reapQueueLock", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    @Transactional
    public void reapQueue() {
        service.reapQueue();
    }

    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "statusUpdateLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runStatusUpdate() {
        service.register("statusUpdateJob", null);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "closedStatusLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runClosedStatus() {
        service.register("statusClosedJob", null);
    }

    @Scheduled(cron = "0 0 2 1 7 ?")
    @SchedulerLock(name = "deleteRateLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runDeleteRate() {
        service.register("rateDeleteJob", null);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "dataClosedLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runDataClosed() {
        service.register("dataClosedJob", null);
    }

    @Scheduled(cron = "0 0 4 16 * ?")
    @SchedulerLock(name = "createReportLock", lockAtLeastFor = "1m", lockAtMostFor = "2h")
    public void runCreateReport() {
        YearMonth targetMon = YearMonth.now().minusMonths(1);

        String citParams = new Gson().toJson(Map.of(
                "reportType", "CIT",
                "endMon", targetMon.toString()
        ));
        service.register("reportCreateJob", citParams);

        String vatParams = new Gson().toJson(Map.of(
                "reportType", "VAT",
                "endMon", targetMon.toString()
        ));
        service.register("reportCreateJob", vatParams);
    }
}