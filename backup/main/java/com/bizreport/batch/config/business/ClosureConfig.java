package com.bizreport.batch.config.business;

import com.bizreport.core.dto.business.BizContext;
import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.User;
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
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClosureConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final HistoryRepository historyRepo;

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
                .<User, BizContext>chunk(chunk, manager)
                .reader(new RepositoryItemReaderBuilder<User>()
                        .name("closedReader")
                        .repository(userRepo)
                        .methodName("closedUser")
                        .arguments(LocalDate.now(), Status.CLOSED)
                        .pageSize(chunk)
                        .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                        .build())
                .processor((ItemProcessor<User, BizContext>) user -> {
                    BizHistory current = historyRepo.findFirstByUserOrderByIdDesc(user);
                    if (current != null) current.close(user.getEndDt().plusDays(1));

                    user.batchUpdate(Status.CLOSED, user.getTaxType(), user.getTaxTypeChangeDt(), user.getEndDt());
                    return new BizContext(user, null, user.toHistEntity(user));
                })
                .writer(chunkList -> {
                    for (BizContext ctx : chunkList) {
                        if (ctx.before() != null) historyRepo.save(ctx.before());
                        userRepo.save(ctx.user());
                        historyRepo.save(ctx.after());
                    }
                    log.info(">>>> {}명 자동 폐업 처리 완료", chunkList.size());
                })
                .build();
    }
}