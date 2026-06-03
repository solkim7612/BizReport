package com.bizreport.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {
    private final JobLauncher jobLauncher;
    private final Job statusUpdateJob;
    private final Job statusClosedJob;
    private final Job monReportJob;
    private final Job accReportJob;
    private final Job rateDeleteJob;
    private final Job dataClosedJob;

    public void runUpdateStatus() {

        log.info("[BATCH] 분기별 국세청 상태 전체 동기화");
        executeJob(statusUpdateJob);
    }

    public void runClosedStatus() {

        log.info("[BATCH] 폐업일 경과 사업자 상태 자동 전환");
        executeJob(statusClosedJob);
    }

    public void runReportMonthly() {

        log.info("[BATCH] 월간 리포트 자동 생성");
        executeJob(monReportJob);
    }

    public void runReportAccumulated() {

        log.info("[BATCH] 누적 리포트 자동 생성");
        executeJob(accReportJob);
    }

    public void runDeleteRate() {

        log.info("[BATCH] 지난 세율 데이터 정리");
        executeJob(rateDeleteJob);
    }

    public void runClosedData() {

        log.info("[BATCH] 신고 마감기한 경과 세무 데이터 잠금");
        executeJob(dataClosedJob);
    }

    @CacheEvict(value = {"taxRate", "indNm"}, allEntries = true)
    public void clearRateCache() {

        log.info("[BATCH] 새로운 세율이 적용되어 메모리의 세율 및 업종명 캐시를 모두 초기화");
    }

    private void executeJob(Job job) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(job, params);

        } catch (Exception e) {
            log.error("[BATCH] 해당 배치 작업 실행 실패: {}", job.getName(), e);
        }
    }
}