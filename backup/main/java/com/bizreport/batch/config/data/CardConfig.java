package com.bizreport.batch.config.data;

import com.bizreport.core.dto.data.CardRequest;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.user.User;
import com.bizreport.core.exception.CustomException;
import com.bizreport.core.exception.ErrorCode;
import com.bizreport.core.repository.data.DataJdbcRepository;
import com.bizreport.core.repository.data.DataRepository;
import com.bizreport.core.repository.business.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CardConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final DataRepository dataRepo;
    private final UserRepository userRepo;
    private final DataJdbcRepository jdbcRepo;

    @Value("${batch.chunk.data:500}")
    private int chunk;

    @Value("${dir.upload.card}")
    private String path;

    @Bean
    public Job cardJob() {
        return new JobBuilder("cardJob", job)
                .start(cardStep())
                .build();
    }

    @Bean
    public Step cardStep() {
        return new StepBuilder("cardStep", job)
                .<CardRequest, Data>chunk(chunk, manager)
                .reader(cardReader(null))
                .processor(cardProcessor(null))
                .writer(cardWriter())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CardRequest> cardReader(@Value("#{jobParameters['id']}") String id) {

        String targetDirPath = this.path + id + "/";
        File dir = new File(targetDirPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new IllegalStateException("해당 사업자의 카드 파일(CSV)이 없습니다. 경로: " + targetDirPath);
        }

        return new FlatFileItemReaderBuilder<CardRequest>()
                .name("cardFileReader")
                .resource(new FileSystemResource(files[0]))
                .delimited()
                .names("cardNum", "transDt", "venderId", "netValue", "vatValue", "totalPrice")
                .linesToSkip(2)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{ setTargetType(CardRequest.class); }})
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<CardRequest, Data> cardProcessor(@Value("#{jobParameters['id']}") String id) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return request -> {
            LocalDate transDt = LocalDate.parse(request.getTransDt());
            LocalDate citDeadline = LocalDate.of(transDt.getYear() + 1, 5, 31);

            if (LocalDate.now().isAfter(citDeadline)) return null;

            request.validPrice();

            LocalDate vatDeadline = (transDt.getMonthValue() <= 6)
                    ? LocalDate.of(transDt.getYear(), 7, 25)
                    : LocalDate.of(transDt.getYear() + 1, 1, 25);

            if (request.parsedVatValue().compareTo(BigDecimal.ZERO) > 0 && LocalDate.now().isAfter(vatDeadline)) {
                request.setVatValue("0");
                request.setNetValue(request.getTotalPrice().toString());
            }

            return request.toEntity(user);
        };
    }

    private ItemWriter<Data> cardWriter() {
        return chunkList -> {
            List<Data> dataList = new ArrayList<>(chunkList.getItems());

            jdbcRepo.insert(dataList);
            log.info(">>>> 카드 데이터 {}건 Bulk Insert 완료", dataList.size());
        };
    }
}