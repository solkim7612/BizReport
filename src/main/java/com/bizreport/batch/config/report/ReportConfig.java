package com.bizreport.batch.config.report;

import com.bizreport.api.domain.report.ReportService;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.User;
import com.bizreport.core.repository.business.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReportConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository repository;
    private final ReportService service;

    @Value("${batch.chunk.report:100}")
    private int chunk;

    @Bean
    public Job reportJob() {
        return new JobBuilder("reportJob", job)
                .start(reportStep())
                .build();
    }

    @Bean
    public Step reportStep() {
        return new StepBuilder("reportStep", job)
                .<User, User>chunk(chunk, manager)
                .reader(new RepositoryItemReaderBuilder<User>()
                        .name("reportReader")
                        .repository(repository)
                        .methodName("findAll")
                        .pageSize(chunk)
                        .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                        .build())
                .writer(users -> {
                    List<String> userIds = users.getItems().stream()
                            .map(User::getId)
                            .toList();

                    YearMonth lastMonth = YearMonth.now().minusMonths(1);

                    try {
                        service.generateMonthly(userIds, ReportType.VAT, lastMonth);
                        service.generateMonthly(userIds, ReportType.CIT, lastMonth);
                        log.info(">>>> {}건 월간 리포트 (VAT, CIT) 일괄 생성 완료", userIds.size());
                    } catch (Exception e) {
                        log.error("월간 리포트 벌크 생성 실패 (청크 단위): {}", e.getMessage());
                    }
                })
                .build();
    }
}