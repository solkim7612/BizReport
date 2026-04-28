package com.bizreport.api.config.batch.report;

import com.bizreport.api.domian.report.CITCalcService;
import com.bizreport.api.domian.report.VATCalcService;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.repository.UserRepository;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.YearMonth;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReportConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository userRepo;
    private final VATCalcService vatCalc;
    private final CITCalcService citCalc;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job monthlyReportJob(Step monthlyReportStep) {
        return new JobBuilder("monthlyReportJob", job)
                .start(monthlyReportStep)
                .build();
    }

    @Bean
    public Step monthlyReportStep() {
        return new StepBuilder("monthlyReportStep", job)
                .<User, User>chunk(CHUNK_SIZE, manager)
                .reader(reader())
                .writer(writer())
                .build();
    }

    @Bean
    public RepositoryItemReader<User> reader() {
        return new RepositoryItemReaderBuilder<User>()
                .name("reader")
                .repository(userRepo)
                .methodName("findAll")
                .pageSize(CHUNK_SIZE)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemWriter<User> writer() {
        return users -> {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);

            for (User user : users) {
                try {
                    vatCalc.generateMonthly(user.getId(), lastMonth);
                    citCalc.generateMonthly(user.getId(), lastMonth);
                } catch (Exception e) {
                    log.error("B_NO {} 월간 리포트 생성 실패: {}", user.getId(), e.getMessage());
                }
            }
        };
    }
}