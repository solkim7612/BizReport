package com.bizreport.batch.config.business;

import com.bizreport.core.dto.business.BizContext;
import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.Users;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClosureConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final JdbcTemplate template;

    @Value("${batch.chunk.business:500}")
    private int chunk;

    @Bean
    public Job closedJob() {
        return new JobBuilder("closedJob", job)
                .start(closedStep())
                .build();
    }

    @Bean
    public Step closedStep() {
        return new StepBuilder("closedStep", job)
                .<Users, Users>chunk(chunk, manager)
                .reader(closedReader())
                .writer(closedWriter())
                .build();
    }

    private RepositoryItemReader<Users> closedReader() {
        return new RepositoryItemReaderBuilder<Users>()
                .name("closedReader")
                .repository(userRepo)
                .methodName("closedUser")
                .arguments(LocalDate.now(), Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemWriter<Users> closedWriter() {
        return chunkList -> {
            List<Users> users = new ArrayList<>(chunkList.getItems());

            List<Object[]> historyUpdateArgs = new ArrayList<>();
            List<Object[]> historyInsertArgs = new ArrayList<>();
            List<Object[]> userUpdateArgs = new ArrayList<>();

            for (Users user : users) {
                LocalDate closeDt = user.getEndDt() != null ? user.getEndDt() : LocalDate.now();
                LocalDate newHistStartDt = closeDt.plusDays(1);

                historyUpdateArgs.add(new Object[]{ newHistStartDt, user.getId() });

                historyInsertArgs.add(new Object[]{ user.getId(), user.getTaxType().name(), Status.CLOSED.name(), newHistStartDt });

                userUpdateArgs.add(new Object[]{ Status.CLOSED.name(), user.getId() });
            }

            if (!userUpdateArgs.isEmpty()) {
                template.batchUpdate(
                        "UPDATE BIZ_HISTORY SET tax_type_end_dt = ? WHERE b_id = ? AND tax_type_end_dt = '9999-12-31'",
                        historyUpdateArgs);

                template.batchUpdate(
                        "INSERT INTO BIZ_HISTORY (b_id, tax_type, stt, tax_type_change_dt, tax_type_end_dt) VALUES (?, ?, ?, ?, '9999-12-31')",
                        historyInsertArgs);

                template.batchUpdate(
                        "UPDATE USERS SET stt = ? WHERE b_id = ?",
                        userUpdateArgs);

                log.info(">>>> 상태 변경 및 이력 갱신 Bulk 완료");
            }
        };
    }
}