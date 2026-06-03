package com.bizreport.batch.config.business.rate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RateDeleteConfig {

    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final JdbcTemplate template;

    @Bean
    public Job rateDeleteJob() {
        return new JobBuilder("rateDeleteJob", job)
                .start(rateDeleteStep())
                .build();
    }

    @Bean
    public Step rateDeleteStep() {
        return new StepBuilder("rateDeleteStep", job)
                .tasklet((contribution, chunkContext) -> {

                    int targetYear = LocalDate.now().getYear() - 5;
                    String targetYearStr = String.valueOf(targetYear);
                    log.info("[BATCH] 세율 데이터 정리 시작: {}년 이하 데이터 안전 삭제", targetYearStr);

                    int deletedCount;
                    int totalDeleted = 0;
                    do {
                        deletedCount = template.update(
                                "DELETE FROM TAX_RATE WHERE target_year <= ? LIMIT 1000",
                                targetYearStr
                        );
                        totalDeleted += deletedCount;

                        if (deletedCount > 0) {
                            Thread.sleep(100);
                        }

                    } while (deletedCount > 0);

                    log.info("[BATCH] 세율 데이터 정리 완료: 총 {}건 삭제", totalDeleted);
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}