package com.bizreport.batch.config.business.rate;

import com.bizreport.core.dto.business.RateFileRequest;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.repository.batch.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RateUploadConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final DataSource dataSource;
    private final BatchRepository repository;

    @Value("${batch.chunk.business:500}")
    private int chunk;

    @Bean
    public Job rateUploadJob() {
        return new JobBuilder("rateUploadJob", job)
                .start(rateUploadStep())
                .build();
    }

    @Bean
    public Step rateUploadStep() {
        return new StepBuilder("rateUploadStep", job)
                .<RateFileRequest, TaxRate>chunk(chunk, manager)
                .reader(rateFileReader(null, null))
                .processor(RateFileRequest::toEntity)
                .writer(rateWriter())
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(20)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<RateFileRequest> rateFileReader(
            @Value("#{jobParameters['requestId']}") Long requestId,
            @Value("#{jobParameters['fileName']}") String fileName) {

        BatchRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("대기열에서 데이터를 찾을 수 없습니다. ID: " + requestId));

        Resource resource = new ByteArrayResource(request.getFileData().getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        return new FlatFileItemReaderBuilder<RateFileRequest>()
                .name("rateFileReader")
                .resource(resource)
                .encoding("UTF-8")
                .delimited()
                .names("year", "indCd", "indNm", "category1", "category2", "category3", "msg", "expRt", "overExpRt", "stndExpRt")
                .linesToSkip(3)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(RateFileRequest.class);
                }})
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<TaxRate> rateWriter() {
        String sql = """
                    INSERT INTO TAX_RATE (ind_cd, target_year, ind_nm, vat_rt, exp_rt)
                    VALUES (:id.indCd, :id.year, :indNm, :vatRt, :expRt)
                    ON DUPLICATE KEY UPDATE
                        ind_nm = VALUES(ind_nm),
                        vat_rt = VALUES(vat_rt),
                        exp_rt = VALUES(exp_rt)
                """;

        return new JdbcBatchItemWriterBuilder<TaxRate>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }
}