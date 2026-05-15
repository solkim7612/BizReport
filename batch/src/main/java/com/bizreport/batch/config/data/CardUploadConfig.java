package com.bizreport.batch.config.data;

import com.bizreport.core.dto.data.CardFileRequest;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.batch.BatchRepository;
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
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CardUploadConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final BatchRepository batchRepo;
    private final DataSource dataSource;
    private final JdbcTemplate template;

    @Value("${batch.chunk.data:500}")
    private int chunk;

    @Bean
    public Job cardUploadJob() {
        return new JobBuilder("cardUploadJob", job)
                .start(createTempTableStep())
                .next(cardUploadStep())
                .next(swapStep())
                .next(dropTempTableStep())
                .build();
    }

    @Bean
    public Step createTempTableStep() {
        return new StepBuilder("createTempTableStep", job)
                .tasklet((contribution, chunkContext) -> {
                    String id = (String) chunkContext.getStepContext().getJobParameters().get("id");
                    String tempTableName = "TEMP_DATA_" + id;

                    template.execute("CREATE TABLE IF NOT EXISTS " + tempTableName + " LIKE DATA");
                    template.execute("TRUNCATE TABLE " + tempTableName);

                    log.info("B_NO {} : 동적 격리 테이블 [{}] 생성 완료", id, tempTableName);
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step cardUploadStep() {
        return new StepBuilder("cardUploadStep", job)
                .<CardFileRequest, Data>chunk(chunk, manager)
                .reader(cardFileReader(null, null))
                .processor(cardFileProcessor(null, null, null))
                .writer(tempWriter(null))
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(20)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CardFileRequest> cardFileReader(
            @Value("#{jobParameters['requestId']}") Long requestId,
            @Value("#{jobParameters['fileName']}") String fileName) {

        BatchRequest request = batchRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("대기열 데이터 없음: " + requestId));

        Resource resource = new ByteArrayResource(request.getFileData().getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        return new FlatFileItemReaderBuilder<CardFileRequest>()
                .name("cardFileReader")
                .resource(resource)
                .delimited()
                .names("transDt", "vendorId", "netValue", "vatValue", "totalPrice")
                .linesToSkip(2)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CardFileRequest.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<CardFileRequest, Data> cardFileProcessor(
            @Value("#{jobParameters['id']}") String id,
            @Value("#{jobParameters['cardNum']}") String cardNum,
            @Value("#{jobParameters['ignoreVat']}") String ignoreVatStr) {

        Users user = userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean ignoreVat = Boolean.parseBoolean(ignoreVatStr);

        return request -> {
            request.validPrice();
            return request.toEntity(user, cardNum, ignoreVat);
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<Data> tempWriter(@Value("#{jobParameters['id']}") String id) {
        String tempTableName = "TEMP_DATA_" + id;

        String sql = """
                INSERT INTO %s 
                (b_id, data_type, data_method, is_e, is_mod, card_num, vendor_id, trans_dt, net_value, vat_value, total_price) 
                VALUES 
                (:user.id, :type, :method, :isE, :isMod, :cardNum, :vendorId, :transDt, :netValue, :vatValue, :totalPrice)
                """.formatted(tempTableName);

        return new JdbcBatchItemWriterBuilder<Data>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }

    @Bean
    public Step swapStep() {
        return new StepBuilder("swapStep", job)
                .tasklet((contribution, chunkContext) -> {
                    Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
                    String id = (String) params.get("id");
                    String cardNum = (String) params.get("cardNum");
                    String startDtStr = (String) params.get("startDt");
                    String endDtStr = (String) params.get("endDt");
                    String tempTableName = "TEMP_DATA_" + id;

                    log.info("B_NO {} : 카드({}) 기간({} ~ {}) 데이터 Swap 처리 시작", id, cardNum, startDtStr, endDtStr);

                    String deleteSql = """
                            DELETE FROM DATA 
                            WHERE b_id = ? 
                              AND data_method = 'CARD' 
                              AND card_num = ? 
                              AND trans_dt BETWEEN ? AND ?
                            """;
                    int deleted = template.update(deleteSql, id, cardNum, startDtStr, endDtStr);

                    String insertSql = """
                            INSERT INTO DATA (b_id, data_type, data_method, is_e, is_mod, card_num, vendor_id, trans_dt, net_value, vat_value, total_price) 
                            SELECT b_id, data_type, data_method, is_e, is_mod, card_num, vendor_id, trans_dt, net_value, vat_value, total_price 
                            FROM %s 
                            WHERE trans_dt BETWEEN ? AND ?
                            """.formatted(tempTableName);
                    int inserted = template.update(insertSql, startDtStr, endDtStr);

                    log.info("B_NO {} : 기존 데이터 {}건 삭제 / 신규 데이터 {}건 적재 Swap 완료", id, deleted, inserted);

                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step dropTempTableStep() {
        return new StepBuilder("dropTempTableStep", job)
                .tasklet((contribution, chunkContext) -> {
                    String id = (String) chunkContext.getStepContext().getJobParameters().get("id");
                    String tempTableName = "TEMP_DATA_" + id;

                    template.execute("DROP TABLE IF EXISTS " + tempTableName);
                    log.info("B_NO {} : 동적 격리 테이블 [{}] Drop 정리 완료", id, tempTableName);

                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}