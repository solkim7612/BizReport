package com.bizreport.batch.config.data;

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
public class DataClosedConfig {

    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final JdbcTemplate template;

    @Bean
    public Job dataClosedJob() {
        return new JobBuilder("dataClosedJob", job)
                .start(dataClosedStep())
                .build();
    }

    @Bean
    public Step dataClosedStep() {
        return new StepBuilder("dataClosedStep", job)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate today = LocalDate.now();
                    LocalDate closedDt = null;

                    if (today.isAfter(LocalDate.of(today.getYear(), 7, 25))) {
                        closedDt = LocalDate.of(today.getYear(), 6, 30);
                    } else if (today.isAfter(LocalDate.of(today.getYear(), 5, 31))) {
                        closedDt = LocalDate.of(today.getYear() - 1, 12, 31);
                    } else if (today.isAfter(LocalDate.of(today.getYear(), 1, 25))) {
                        closedDt = LocalDate.of(today.getYear() - 1, 12, 31);
                    }

                    if (closedDt == null) {
                        log.info("[BATCH] 마감 기한이 도래한 데이터가 없습니다.");
                        return RepeatStatus.FINISHED;
                    }

                    log.info("[BATCH] 세무 데이터 마감 시작: {} 이전 데이터 수정 불가 처리", closedDt);

                    int updatedCount;
                    int totalUpdated = 0;
                    do {
                        String sql = """
                                UPDATE DATA 
                                SET is_mod = false, updated_at = CURRENT_TIMESTAMP
                                WHERE is_mod = true AND trans_dt <= ? 
                                LIMIT 1000
                                """;

                        updatedCount = template.update(sql, closedDt.toString());
                        totalUpdated += updatedCount;

                        if (updatedCount > 0) {
                            Thread.sleep(100);
                        }

                    } while (updatedCount > 0);

                    log.info("[BATCH] 세무 데이터 마감 완료: 총 {}건 잠금 처리됨", totalUpdated);
                    return RepeatStatus.FINISHED;

                }, manager)
                .build();
    }
}