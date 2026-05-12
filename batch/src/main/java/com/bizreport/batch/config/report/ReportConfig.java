package com.bizreport.batch.config.report;

import com.bizreport.api.domain.report.ReportService;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.repository.business.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.YearMonth;
import java.util.ArrayList;
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
    private final JdbcTemplate template;
    private final ObjectMapper mapper;

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
                .<Users, List<Reports>>chunk(chunk, manager)
                .reader(reportReader())
                .processor(reportProcessor())
                .writer(reportWriter())
                .build();
    }

    private RepositoryItemReader<Users> reportReader() {
        return new RepositoryItemReaderBuilder<Users>()
                .name("reportReader")
                .repository(repository)
                .methodName("findBySttNot")
                .arguments(Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Users, List<Reports>> reportProcessor() {
        return user -> {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            try {
                return service.calculateMonthly(user, lastMonth);
            } catch (Exception e) {
                log.warn("B_NO {} 리포트 생성 실패 (Skip): {}", user.getId(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<List<Reports>> reportWriter() {
        return chunkList -> {
            List<Object[]> batchArgs = new ArrayList<>();

            for (List<Reports> userReports : chunkList.getItems()) {
                for (Reports report : userReports) {
                    try {
                        String calcJson = mapper.writeValueAsString(report.getCalc());

                        batchArgs.add(new Object[]{
                                report.getUser().getId(),
                                report.getReportType().name(),
                                report.getPeriodType().name(),
                                report.getPeriod(),
                                report.getResult(),
                                calcJson
                        });
                    } catch (JsonProcessingException e) {
                        log.error("JSON 파싱 에러", e);
                    }
                }
            }

            if (!batchArgs.isEmpty()) {
                String sql = """
                    INSERT INTO REPORTS (b_id, report_type, period_type, period_target, tax_result, tax_calc)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        tax_result = VALUES(tax_result),
                        tax_calc = VALUES(tax_calc),
                        updated_at = CURRENT_TIMESTAMP
                """;

                template.batchUpdate(sql, batchArgs);
                log.info(">>>> {}건 월간 리포트 (VAT, CIT) Bulk Upsert 완료", batchArgs.size());
            }
        };
    }
}