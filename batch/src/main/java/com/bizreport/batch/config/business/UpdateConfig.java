package com.bizreport.batch.config.business;

import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.Users;
import com.bizreport.api.config.api.APIClient;
import com.bizreport.core.dto.business.BizContext;
import com.bizreport.core.repository.business.HistoryRepository;
import com.bizreport.core.repository.business.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UpdateConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final APIClient client;
    private final JdbcTemplate template;

    @Value("${batch.chunk.api:100}")
    private int chunk;

    @Bean
    public Job updateJob(){
        return new JobBuilder("updateJob", job)
                .start(updateStep())
                .build();
    }

    @Bean
    public Step updateStep(){
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);

        return new StepBuilder("updateStep", job)
                .<Users, Users>chunk(chunk, manager)
                .reader(userReader())
                .writer(updateWriter())
                .faultTolerant()
                .retry(CustomException.class)
                .retryLimit(3)
                .backOffPolicy(backOffPolicy)
                .build();
    }

    private RepositoryItemReader<Users> userReader() {
        return new RepositoryItemReaderBuilder<Users>()
                .name("updateReader")
                .repository(userRepo)
                .methodName("findBySttNot")
                .arguments(Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemWriter<Users> updateWriter() {
        return chunkList -> {
            List<Users> users = new ArrayList<>(chunkList.getItems());
            List<String> bNoList = users.stream().map(Users::getId).toList();

            List<StatusResponse.Data> dataList = client.status(bNoList);

            List<Object[]> historyUpdateArgs = new ArrayList<>();
            List<Object[]> historyInsertArgs = new ArrayList<>();
            List<Object[]> userUpdateArgs = new ArrayList<>();

            for (Users user : users) {
                StatusResponse.Data ntsData = dataList.stream()
                        .filter(d -> d.getB_no().equals(user.getId()))
                        .findFirst().orElse(null);

                if (ntsData == null || "국세청에 등록되지 않은 사업자등록번호입니다.".equals(ntsData.getTax_type())) continue;

                TaxType newTaxType = TaxType.ofCode(ntsData.getTax_type_cd());
                Status newStt = Status.ofCode(ntsData.getB_stt_cd());

                if (user.getTaxType() == newTaxType && user.getStt() == newStt) continue;

                log.info("B_NO {} : 상태 변동 감지 업데이트 ({} -> {})", user.getId(), user.getStt(), newStt);

                LocalDate changeDt = ntsData.parseDate(ntsData.getTax_type_change_dt());
                if (changeDt == null) changeDt = LocalDate.now();

                historyUpdateArgs.add(new Object[]{ changeDt, user.getId() });

                historyInsertArgs.add(new Object[]{ user.getId(), newTaxType.name(), newStt.name(), changeDt });

                userUpdateArgs.add(new Object[]{ newTaxType.name(), newStt.name(), user.getId() });
            }

            if (!userUpdateArgs.isEmpty()) {
                template.batchUpdate(
                        "UPDATE BIZ_HISTORY SET tax_type_end_dt = ? WHERE b_id = ? AND tax_type_end_dt = '9999-12-31'",
                        historyUpdateArgs);

                template.batchUpdate(
                        "INSERT INTO BIZ_HISTORY (b_id, tax_type, stt, tax_type_change_dt, tax_type_end_dt) VALUES (?, ?, ?, ?, '9999-12-31')",
                        historyInsertArgs);

                template.batchUpdate(
                        "UPDATE USERS SET tax_type = ?, stt = ? WHERE b_id = ?",
                        userUpdateArgs);

                log.info(">>>> 상태 변경 및 이력 갱신 Bulk 완료");
            }
        };
    }
}