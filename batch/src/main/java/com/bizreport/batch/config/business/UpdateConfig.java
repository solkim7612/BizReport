package com.bizreport.batch.config.business;

import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.User;
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
public class UpdateConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final HistoryRepository historyRepo;
    private final APIClient client;

    @Value("${batch.chunk.business:500}")
    private int chunk;

    @Bean
    public Job updateJob(){
        return new JobBuilder("updateJob", job)
                .start(updateStep())
                .build();
    }

    @Bean
    public Step updateStep(){
        return new StepBuilder("updateStep", job)
                .<User, BizContext>chunk(chunk, manager)
                .reader(userReader())
                .processor(userProcessor())
                .writer(contextWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(1000)
                .build();
    }

    private RepositoryItemReader<User> userReader() {
        return new RepositoryItemReaderBuilder<User>()
                .name("updateReader")
                .repository(userRepo)
                .methodName("findAll")
                .arguments(Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    private ItemProcessor<User, BizContext> userProcessor() {
        return user -> {
            StatusResponse.Data data = client.status(user.getId());
            TaxType taxType = TaxType.ofCode(data.getTax_type_cd());
            Status stt = Status.ofCode(data.getB_stt_cd());

            if (user.getTaxType() == taxType && user.getStt() == stt) return null;

            LocalDate taxTypeChangeDt = data.parseDate(data.getTax_type_change_dt());
            BizHistory before = historyRepo.findFirstByUserOrderByIdDesc(user);
            if (before != null) before.close(taxTypeChangeDt);

            data.batchUpdate(user);
            BizHistory after = user.toHistEntity(user);

            log.info("B_NO {} : 변동 감지 (Processor 통과)", user.getId());
            return new BizContext(user, before, after);
        };
    }

    private ItemWriter<BizContext> contextWriter() {
        return chunk -> {
            for (BizContext ctx : chunk) {
                if (ctx.before() != null) historyRepo.save(ctx.before());
                userRepo.save(ctx.user());
                historyRepo.save(ctx.after());
            }
            log.info(">>>> {}건의 변경사항 DB 저장 완료", chunk.size());
        };
    }
}