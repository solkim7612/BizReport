package com.bizreport.batch.config.business.status;

import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.Users;
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
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StatusClosedConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final JdbcTemplate template;

    @Value("${batch.chunk.business:500}")
    private int chunk;

    @Bean
    public Job statusClosedJob() {
        return new JobBuilder("statusClosedJob", job)
                .start(statusClosedStep())
                .build();
    }

    @Bean
    public Step statusClosedStep() {
        return new StepBuilder("statusClosedStep", job)
                .<Users, Users>chunk(chunk, manager)
                .reader(closedUserReader())
                .writer(closedUserWriter())
                .build();
    }

    private RepositoryItemReader<Users> closedUserReader() {
        return new RepositoryItemReaderBuilder<Users>()
                .name("closedUserReader")
                .repository(userRepo)
                .methodName("findUsersToClose")
                .arguments(LocalDate.now(), Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemWriter<Users> closedUserWriter() {
        return chunkList -> {
            List<Users> users = new ArrayList<>(chunkList.getItems());

            List<Object[]> historyUpdateArgs = new ArrayList<>();
            List<Object[]> userUpdateArgs = new ArrayList<>();

            for (Users user : users) {
                LocalDate closeDt = user.getEndDt() != null ? user.getEndDt() : LocalDate.now();

                historyUpdateArgs.add(new Object[]{ closeDt, user.getId() });

                userUpdateArgs.add(new Object[]{ Status.CLOSED.name(), user.getId() });
            }

            if (!userUpdateArgs.isEmpty()) {
                String historySql = """
                        UPDATE BIZ_HISTORY 
                        SET tax_type_end_dt = ? 
                        WHERE b_id = ? AND tax_type_end_dt = '9999-12-31'
                        """;
                template.batchUpdate(historySql, historyUpdateArgs);

                String userSql = "UPDATE USERS SET b_stt = ? WHERE b_id = ?";
                template.batchUpdate(userSql, userUpdateArgs);

                log.info(">>>> {}건의 폐업 사업자 상태 전환 및 이력 마감(Bulk) 완료", userUpdateArgs.size());
            }
        };
    }
}