package com.bizreport.batch.config.report;

import com.bizreport.core.service.report.ReportService;
import com.bizreport.core.dto.report.ReportCommand;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.ReportType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AccReportConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final UserRepository repository;
    private final ReportService service;
    private final JdbcTemplate template;
    private final ObjectMapper mapper;

    @Value("${batch.chunk.report:100}")
    private int chunk;

    @Bean
    public Job accReportJob() {
        return new JobBuilder("accReportJob", job)
                .start(accReportStep())
                .build();
    }

    @Bean
    public Step accReportStep() {
        return new StepBuilder("accumulatedReportStep", job)
                .<Users, List<ReportResponse>>chunk(chunk, manager)
                .reader(accReportReader())
                .processor(accReportProcessor())
                .writer(accReportWriter())
                .build();
    }

    private RepositoryItemReader<Users> accReportReader() {
        return new RepositoryItemReaderBuilder<Users>()
                .name("accumulatedReportReader")
                .repository(repository)
                .methodName("findBySttNot")
                .arguments(Status.CLOSED)
                .pageSize(chunk)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Users, List<ReportResponse>> accReportProcessor() {
        return user -> {
            LocalDate today = LocalDate.now();
            List<ReportResponse> results = new ArrayList<>();

            try {
                if (today.getMonthValue() == 7) {
                    ReportCommand command = new ReportCommand(user, ReportType.VAT, PeriodType.ACCUMULATED,
                            YearMonth.of(today.getYear(), 1), YearMonth.of(today.getYear(), 6), BigDecimal.ZERO);

                    results.add(service.generateReport(command));

                } else if (today.getMonthValue() == 1) {
                    ReportCommand command = new ReportCommand(user, ReportType.VAT, PeriodType.ACCUMULATED,
                            YearMonth.of(today.getYear() - 1, 7), YearMonth.of(today.getYear() - 1, 12), BigDecimal.ZERO);

                    results.add(service.generateReport(command));

                } else if (today.getMonthValue() == 5) {
                    ReportCommand command = new ReportCommand(user, ReportType.CIT, PeriodType.ACCUMULATED,
                            YearMonth.of(today.getYear() - 1, 1), YearMonth.of(today.getYear() - 1, 12), BigDecimal.ZERO);

                    results.add(service.generateReport(command));
                }

            } catch (Exception e) {
                log.warn("[BATCH] B_NO {} 누적 리포트 강제 마감 실패: {}", user.getId(), e.getMessage());
            }

            return results.isEmpty() ? null : results;
        };
    }

    @Bean
    public ItemWriter<List<ReportResponse>> accReportWriter() {
        return chunkList -> {
            List<Object[]> reportArgs = new ArrayList<>();

            for (List<ReportResponse> reports : chunkList.getItems()) {
                for (ReportResponse report : reports) {
                    try {
                        String calcJson = mapper.writeValueAsString(report.getCalc());

                        reportArgs.add(new Object[]{
                                report.getUserId(),
                                report.getReportType().name(),
                                report.getPeriodType().name(),
                                report.getPeriod(),
                                report.getTax(),
                                calcJson
                        });

                    } catch (JsonProcessingException e) {
                        log.error("[BATCH] JSON 파싱 에러", e);
                    }
                }
            }

            if (!reportArgs.isEmpty()) {
                String sql = """
                    INSERT INTO REPORTS (b_id, report_type, period_type, period_target, tax_result, tax_calc)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        tax_result = VALUES(tax_result),
                        tax_calc = VALUES(tax_calc),
                        updated_at = CURRENT_TIMESTAMP
                """;

                template.batchUpdate(sql, reportArgs);
                log.info("[BATCH] 월간 리포트 (VAT, CIT) Upsert 완료: {}건", reportArgs.size());
            }
        };
    }
}
