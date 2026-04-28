package com.bizreport.api.config.batch.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TaxRateConfig {

    private final JobRepository job;
    private final PlatformTransactionManager manager;

    @Bean
    public Job taxRateJob(Step taxRateStep) {
        return new JobBuilder("taxRateJob", job)
                .start(taxRateStep)
                .build();
    }

    @Bean
    public Step taxRateStep() {
        return new StepBuilder("taxRateStep", job)
                .tasklet((contribution, chunkContext) -> {
                    log.info("국세청 세율/경비율 데이터 수집 배치가 동작합니다.");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}