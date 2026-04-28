package com.bizreport.api.config.batch.upload;

import com.bizreport.api.dto.data.RateRequest;
import com.bizreport.api.entity.rate.TaxRate;
import com.bizreport.api.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RateConfig {

    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final TaxRateRepository repository;

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Job rateJob(Step rateStep) {
        return new JobBuilder("rateJob", job)
                .start(rateStep)
                .build();
    }

    @Bean
    public Step rateStep() {
        return new StepBuilder("rateStep", job)
                .<RateRequest, TaxRate>chunk(CHUNK_SIZE, manager)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<RateRequest> reader(@Value("${batch.upload.rate-dir}") String rateDir) {
        File dir = new File(rateDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new IllegalStateException("해당 디렉토리에 CSV 파일이 존재하지 않습니다: " + rateDir);
        }

        File targetFile = files[0];
        log.info("TaxRate 업로드를 시작합니다: {}", targetFile.getAbsolutePath());

        return new FlatFileItemReaderBuilder<RateRequest>()
                .name("rateReader")
                .resource(new FileSystemResource(targetFile))
                .delimited()
                .names("year", "indCd", "indNm", "category1", "category2", "category3", "msg", "expRate", "overExpRate", "stndExpRate")
                .linesToSkip(3)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(RateRequest.class);
                }})
                .build();
    }

    @Bean
    public ItemProcessor<RateRequest, TaxRate> processor() {
        return RateRequest::toEntity;
    }

    @Bean
    public RepositoryItemWriter<TaxRate> writer() {
        return new RepositoryItemWriterBuilder<TaxRate>()
                .repository(repository)
                .methodName("save")
                .build();
    }
}