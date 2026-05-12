package com.bizreport.batch.config.data;

import com.bizreport.core.dto.data.CardRequest;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CardConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final BatchRepository batchRepo;
    private final DataSource dataSource;
    private final JdbcTemplate template;

    @Value("${batch.chunk.data:500}")
    private int chunk;

    @Bean
    public Job cardJob() {
        return new JobBuilder("cardJob", job)
                .start(createTempTableStep())
                .next(cardStep())
                .next(swapDataStep())
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
    public Step cardStep() {
        return new StepBuilder("cardStep", job)
                .<CardRequest, Data>chunk(chunk, manager)
                .reader(cardReader(null, null))
                .processor(cardProcessor(null))
                .writer(cardTempWriter(null))
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(20)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CardRequest> cardReader(
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

        return new FlatFileItemReaderBuilder<CardRequest>()
                .name("cardFileReader")
                .resource(resource)
                .delimited()
                .names("cardNum", "transDt", "venderId", "netValue", "vatValue", "totalPrice")
                .linesToSkip(2)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CardRequest.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<CardRequest, Data> cardProcessor(@Value("#{jobParameters['id']}") String id) {

        Users user = userRepo.findById(id)
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

    @Bean
    @StepScope
    public JdbcBatchItemWriter<Data> cardTempWriter(@Value("#{jobParameters['id']}") String id) {
        String tempTableName = "TEMP_DATA_" + id;

        String sql = "INSERT INTO " + tempTableName + " " +
                "(b_id, data_type, data_method, is_e, is_mod, card_num, vendor_id, trans_dt, net_value, vat_value, total_price) " +
                "VALUES (:user.id, :type, :method, :isE, :isMod, :cardNum, :vendorId, :transDt, :netValue, :vatValue, :totalPrice)";

        return new JdbcBatchItemWriterBuilder<Data>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }

    @Bean
    public Step swapDataStep() {
        return new StepBuilder("swapDataStep", job)
                .tasklet((contribution, chunkContext) -> {
                    Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
                    String id = (String) params.get("id");
                    String cardNum = (String) params.get("cardNum");
                    String startDtStr = (String) params.get("startDt");
                    String endDtStr = (String) params.get("endDt");
                    String tempTableName = "TEMP_DATA_" + id;

                    log.info("B_NO {} : 카드({}) 기간({} ~ {}) 데이터 Swap 처리 시작", id, cardNum, startDtStr, endDtStr);

                    String deleteOldDataSql = "DELETE FROM DATA WHERE b_id = ? AND data_method = 'CARD' AND card_num = ? AND trans_dt BETWEEN ? AND ?";
                    int deleted = template.update(deleteOldDataSql, id, cardNum, startDtStr, endDtStr);

                    String insertNewDataSql = "INSERT INTO DATA (b_id, data_type, data_method, is_e, is_mod, card_num, vendor_id, trans_dt, net_value, vat_value, total_price) " +
                            "SELECT b_id, data_type, data_method, is_e, is_mod, card_num, vendor_id, trans_dt, net_value, vat_value, total_price " +
                            "FROM " + tempTableName + " " +
                            "WHERE trans_dt BETWEEN ? AND ?";
                    int inserted = template.update(insertNewDataSql, startDtStr, endDtStr);

                    template.execute("DROP TABLE IF EXISTS " + tempTableName);

                    log.info("B_NO {} : 기존 카드 데이터 {}건 삭제 / 신규 데이터 {}건 적재 Swap 완료", id, deleted, inserted);

                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}