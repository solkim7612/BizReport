package com.bizreport.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {
    private final JobLauncher asyncJobLauncher;
    private final Job updateJob;
    private final Job reportJob;
    private final Job closedJob;

    public void runUpdate() {
        log.info("BATCH START: 분기별 국세청 상태 전체 동기화");
        executeJob(updateJob);
    }

    public void runClosed() {
        log.info("BATCH START: 폐업일 경과 사업자 상태 자동 전환");
        executeJob(closedJob);
    }

    public void runReport() {
        log.info("BATCH START: 월간 리포트 자동 생성");
        executeJob(reportJob);
    }

    private void executeJob(Job job) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            asyncJobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("배치 작업 실행 실패: {}", job.getName(), e);
        }
    }
}