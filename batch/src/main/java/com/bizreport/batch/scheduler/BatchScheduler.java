package com.bizreport.batch.scheduler;

import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import com.bizreport.core.repository.batch.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
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

    // TODO: Slack API 연동예정

    @Scheduled(fixedDelayString = "60000")
    @SchedulerLock(name = "rateJobLock", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    public void executeBatch() {
        List<BatchRequest> pendingRequests = repository.findByStatusOrderByCreatedAtAsc(BatchStatus.READY);

        if (pendingRequests.isEmpty()) return;

        for (BatchRequest request : pendingRequests) {
            try {
                request.startProcessing();
                repository.saveAndFlush(request);

                JobParametersBuilder builder = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .addString("fileName", request.getFileName())
                        .addLong("requestId", request.getId());

                if (request.getJobParameters() != null) {
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> paramMap = new Gson().fromJson(request.getJobParameters(), type);
                    paramMap.forEach(builder::addString);
                }

                Job jobToExecute;
                if ("rateJob".equals(request.getJobName())) {
                    jobToExecute = rateJob;
                } else if ("cardJob".equals(request.getJobName())) {
                    jobToExecute = cardJob;
                } else {
                    throw new IllegalArgumentException("알 수 없는 Job Name 입니다: " + request.getJobName());
                }

                log.info("대기 중인 배치 작업 발견. 실행 시작 - Job: {}, File: {}", request.getJobName(), request.getFileName());

                jobLauncher.run(jobToExecute, builder.toJobParameters());

                request.complete();
                log.info("배치 작업 성공 - Request ID: {}", request.getId());

            } catch (Exception e) {
                request.fail();
                log.error("배치 작업 실패 - Request ID: {}", request.getId(), e);
            }
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
}