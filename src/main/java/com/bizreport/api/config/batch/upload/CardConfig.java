package com.bizreport.api.config.batch.upload;

import com.bizreport.api.dto.data.CardRequest;
import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import com.bizreport.api.repository.DataRepository;
import com.bizreport.api.repository.UserRepository;
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
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CardConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final DataRepository dataRepo;
    private final UserRepository userRepo;

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Job cardJob(Step cardStep) {
        return new JobBuilder("cardStep", job)
                .start(cardStep)
                .build();
    }

    @Bean
    public Step cardStep() {
        return new StepBuilder("cardStep", job)
                .<CardRequest, Data>chunk(CHUNK_SIZE, manager)
                .reader(reader(null, null))
                .processor(processor(null))
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CardRequest> reader(
            @Value("${batch.upload.card-dir}") String cardDir,
            @Value("#{jobParameters['id']}") String id) {

        String path = cardDir + id + "/";
        File dir = new File(path);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new IllegalStateException("해당 사업자의 카드 파일(CSV)이 없습니다.");
        }

        return new FlatFileItemReaderBuilder<CardRequest>()
                .name("cardReader")
                .resource(new FileSystemResource(files[0]))
                .delimited()
                .names("cardNum", "transDate", "venderId", "netValue", "vatValue", "totalPrice")
                .linesToSkip(2)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CardRequest.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<CardRequest, Data> processor(@Value("#{jobParameters['id']}") String id) {
        return request -> {
            User user = userRepo.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            LocalDate transDate = LocalDate.parse(request.getTransDate());

            LocalDate citDeadline = LocalDate.of(transDate.getYear() + 1, 5, 31);
            if (LocalDate.now().isAfter(citDeadline)) {
                log.warn("B_NO {} : 종합소득세 마감일({}) 경과", id, citDeadline);
                return null;
            }

            LocalDate vatDeadline = (transDate.getMonthValue() <= 6)
                    ? LocalDate.of(transDate.getYear(), 7, 25)
                    : LocalDate.of(transDate.getYear() + 1, 1, 25);

            boolean isVatTarget = request.parsedVatValue().compareTo(BigDecimal.ZERO) > 0;

            if (isVatTarget && LocalDate.now().isAfter(vatDeadline)) {
                log.info("B_NO {} : 부가세 경비 부적격. 종합소득세 경비로 전환", id);
                request.setVatValue("0");
                request.setNetValue(request.getTotalPrice().toString());
            }

            return request.toEntity(user);
        };
    }

    @Bean
    public RepositoryItemWriter<Data> writer() {
        return new RepositoryItemWriterBuilder<Data>()
                .repository(dataRepo)
                .methodName("save")
                .build();
    }
}
