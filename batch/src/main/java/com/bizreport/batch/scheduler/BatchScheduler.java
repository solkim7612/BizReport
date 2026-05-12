package com.bizreport.batch.scheduler;

import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import com.bizreport.core.repository.batch.BatchRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {
    private final BatchRepository repository;
    private final JobLauncher jobLauncher;
    private final Job rateJob;
    private final Job cardJob;
    private final BatchService service;

    @Scheduled(fixedDelayString = "60000")
    @SchedulerLock(name = "executeBatchQueueLock", lockAtLeastFor = "10s", lockAtMostFor = "30m")
    public void executeBatch() {
        List<BatchRequest> pendingRequests = repository.findByStatusOrderByCreatedAtAsc(BatchStatus.READY);

        if (!pendingRequests.isEmpty()) {
            log.info(">>>> 대기열 처리 시작: 총 {}건의 Batch Request 발견", pendingRequests.size());
        }

        for (BatchRequest request : pendingRequests) {
            try {
                request.startProcessing();
                repository.saveAndFlush(request);

                JobParametersBuilder builder = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .addString("fileName", request.getFileName())
                        .addLong("requestId", request.getId());

                if (request.getJobParameters() != null) {
                    Map<String, String> paramMap = new Gson().fromJson(request.getJobParameters(), new TypeToken<Map<String, String>>(){}.getType());
                    paramMap.forEach(builder::addString);
                }

                Job jobToExecute = "rateJob".equals(request.getJobName()) ? rateJob : cardJob;

                JobExecution execution = jobLauncher.run(jobToExecute, builder.toJobParameters());

                if (execution.getStatus().isUnsuccessful()) {
                    request.fail();
                    log.error("배치 큐 처리 실패 (Batch Internal Error) [ID: {}]", request.getId());
                } else {
                    request.complete();
                    log.info("배치 큐 처리 완료 [ID: {}]", request.getId());
                }
                repository.saveAndFlush(request);

            } catch (Exception e) {
                request.fail();
                repository.saveAndFlush(request);
                log.error("배치 큐 런타임 에러 [ID: {}]: {}", request.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "zombieReaperLock", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    @Transactional
    public void reapZombieQueues() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);

        int recoveredCount = repository.recoverZombieRequests(threshold);

        if (recoveredCount > 0) {
            log.warn("[장애 복구] 서버 다운으로 멈춰있던 좀비 큐 {}건을 READY 상태로 롤백(재시도) 처리했습니다.", recoveredCount);
        }
    }

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

    @Scheduled(cron = "0 0 3 1 7 ?")
    @SchedulerLock(name = "runRateCleanupLock", lockAtLeastFor = "1m", lockAtMostFor = "1h")
    public void runCleanup() {
        try {
            service.runCleanup();
        } catch (Exception e) {
            log.error("[BATCH ERROR] 연간 세율 데이터 정리 실패", e);
        }
    }
}