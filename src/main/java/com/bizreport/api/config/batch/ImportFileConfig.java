package com.bizreport.api.config.batch;

import com.bizreport.api.dto.admin.ImportFileRequest;
import com.bizreport.api.entity.rate.TaxRate;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ImportFileConfig {
    private final JobRepository repository;
    private final PlatformTransactionManager manager;
    private final EntityManagerFactory factory;

    @Bean
    public Job importJob(Step step) {
        return new JobBuilder("importJob", repository)
                .start(step)
                .build();
    }

    @Bean
    public Step importStep() {
        return new StepBuilder("importStep", repository)
                .<ImportFileRequest, TaxRate>chunk(100, manager)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ImportFileRequest> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<ImportFileRequest>()
                .name("reader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(3)
                .recordSeparatorPolicy(new DefaultRecordSeparatorPolicy())
                .delimited()
                .names("targetYear", "indCode", "indName", "category1", "category2", "category3", "content", "expRate", "overRate", "baseRate")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(ImportFileRequest.class);
                }})
                .build();
    }

    @Bean
    public ItemProcessor<ImportFileRequest, TaxRate> processor() {
        return request -> {
            try {
                return request.toEntity();
            } catch (Exception e) {
                log.error("PARSING_ERROR: indCode {}, message {}", request.getIndCode(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    public JpaItemWriter<TaxRate> writer() {
        return new JpaItemWriterBuilder<TaxRate>()
                .entityManagerFactory(factory)
                .build();
    }
}
