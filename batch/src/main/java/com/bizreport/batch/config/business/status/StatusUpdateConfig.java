package com.bizreport.batch.config.business.status;

import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.config.NTSClient;
import com.bizreport.core.repository.business.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
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
public class StatusUpdateConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final NTSClient client;
    private final JdbcTemplate template;

    @Value("${batch.chunk.api:100}")
    private int chunk;

    @Bean
    public Job statusUpdateJob() {
        return new JobBuilder("statusUpdateJob", job)
                .start(statusUpdateStep())
                .build();
    }

    @Bean
    public Step statusUpdateStep() {
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);

        return new StepBuilder("statusUpdateStep", job)
                .<Users, Users>chunk(chunk, manager)
                .reader(updateUserReader())
                .writer(updateUserWriter())
                .faultTolerant()
                .retry(CustomException.class)
                .retryLimit(3)
                .backOffPolicy(backOffPolicy)
                .skip(CustomException.class)
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    private RepositoryItemReader<Users> updateUserReader() {
        return new RepositoryItemReaderBuilder<Users>()
                .name("updateUserReader")
                .repository(userRepo)
                .methodName("findBySttNot")
                .arguments(Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemWriter<Users> updateUserWriter() {
        return chunkList -> {
            List<Users> users = new ArrayList<>(chunkList.getItems());

            List<String> userList = users.stream().map(Users::getId).toList();
            List<StatusResponse.Data> dataList = client.status(userList);

            List<Object[]> historyUpdateArgs = new ArrayList<>();
            List<Object[]> historyInsertArgs = new ArrayList<>();
            List<Object[]> userUpdateArgs = new ArrayList<>();

            for (Users user : users) {

                StatusResponse.Data data = dataList.stream()
                        .filter(d -> d.getB_no().equals(user.getId()))
                        .findFirst().orElse(null);
                if (data == null || "국세청에 등록되지 않은 사업자등록번호입니다.".equals(data.getTax_type())) continue;

                TaxType newTaxType = TaxType.ofCode(data.getTax_type_cd());
                Status newStt = Status.ofCode(data.getB_stt_cd());

                boolean isTaxTypeChanged = user.getTaxType() != newTaxType;
                boolean isSttChanged = user.getStt() != newStt;
                if (!isTaxTypeChanged && !isSttChanged) continue;

                log.info("[BATCH] B_NO {} 의 상태 변동 감지 : TaxType: {}->{}, Status: {}->{}",
                        user.getId(), user.getTaxType(), newTaxType, user.getStt(), newStt);

                LocalDate changeDt = data.parseDate(data.getTax_type_change_dt());
                LocalDate endDt = data.parseDate(data.getEnd_dt());

                if (isTaxTypeChanged) {
                    LocalDate histChangeDt = changeDt != null ? changeDt : LocalDate.now();
                    LocalDate histEndDt = histChangeDt.minusDays(1);

                    historyUpdateArgs.add(new Object[]{histEndDt, user.getId()});
                    historyInsertArgs.add(new Object[]{user.getId(), newTaxType.name(), histChangeDt});
                }

                userUpdateArgs.add(new Object[]{newTaxType.name(), newStt.name(), changeDt, endDt, user.getId()});
            }

            if (!historyUpdateArgs.isEmpty()) {
                template.batchUpdate(
                        "UPDATE BIZ_HISTORY SET tax_type_end_dt = ? WHERE b_id = ? AND tax_type_end_dt = '9999-12-31'",
                        historyUpdateArgs);

                template.batchUpdate(
                        "INSERT INTO BIZ_HISTORY (b_id, tax_type, tax_type_change_dt, tax_type_end_dt) VALUES (?, ?, ?, '9999-12-31')",
                        historyInsertArgs);

                log.info("[BATCH] 과세유형 변동이력(HISTORY) 갱신 완료: {}건", historyUpdateArgs.size());
            }

            if (!userUpdateArgs.isEmpty()) {
                template.batchUpdate(
                        "UPDATE USERS SET tax_type = ?, b_stt = ?, tax_type_change_dt = ?, end_dt = ? WHERE b_id = ?",
                        userUpdateArgs);
                log.info("[BATCH] 사용자 정보(USERS) 갱신 완료: {}건", userUpdateArgs.size());
            }
        };
    }
}