package com.bizreport.batch.config.report;

import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.service.report.ReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReportCreateConfig {
    private final JobRepository job;
    private final PlatformTransactionManager manager;
    private final EntityManagerFactory emf;
    private final ReportService service;
    private final JdbcTemplate template;
    private final ObjectMapper mapper;

    @Value("${batch.chunk.report:100}")
    private int chunk;

    @Bean
    public Job reportCreateJob() {
        return new JobBuilder("reportCreateJob", job)
                .start(reportCreateStep())
                .build();
    }

    @Bean
    public Step reportCreateStep() {
        return new StepBuilder("reportCreateStep", job)
                .<Users, List<ReportResponse>>chunk(chunk, manager)
                .reader(reportCreateReader())
                .processor(reportCreateProcessor(null, null, null))
                .writer(reportCreateWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<Users> reportCreateReader() {
        return new JpaCursorItemReaderBuilder<Users>()
                .name("reportCreateReader")
                .entityManagerFactory(emf)
                .queryString("SELECT u FROM Users u WHERE u.stt != :status ORDER BY u.id ASC")
                .parameterValues(Map.of("status", Status.CLOSED))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Users, List<ReportResponse>> reportCreateProcessor(
            @Value("#{jobParameters['id']}") String targetId,
            @Value("#{jobParameters['endMon']}") String endMonStr,
            @Value("#{jobParameters['reportType']}") String reportTypeStr) {

        return user -> {
            if (targetId != null && !targetId.isBlank() && !user.getId().equals(targetId)) {
                return null;
            }

            if (user.getIndCd() == null || "미분류".equals(user.getIndCd())) {
                log.warn("[BATCH] B_NO {} 업종 코드 누락으로 리포트 생성 스킵", user.getId());
                return null;
            }

            try {
                ReportType reportType = ReportType.valueOf(reportTypeStr);
                ReportRequest request = new ReportRequest(reportType, endMonStr, BigDecimal.ZERO);
                return service.create(user.getId(), request);

            } catch (Exception e) {
                log.error("[BATCH] 리포트 생성 실패 (B_NO: {}): {}", user.getId(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<List<ReportResponse>> reportCreateWriter() {
        return chunkList -> {
            List<Object[]> reportArgs = new ArrayList<>();

            for (List<ReportResponse> responses : chunkList.getItems()) {
                for (ReportResponse report : responses) {
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
                        log.error("[BATCH] JSON 파싱 에러 (B_NO: {})", report.getUserId(), e);
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
                log.info("[BATCH] 월간/누적 리포트 동시 Upsert 완료: {}건", reportArgs.size());
            }
        };
    }
}