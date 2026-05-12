package com.bizreport.core.repository.report;

import com.bizreport.core.entity.report.Reports;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void insert(List<Reports> reports) {
        String sql = "INSERT INTO REPORTS (b_id, report_type, period_type, period_target, tax_result, tax_calc) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "tax_result = VALUES(tax_result), " +
                "tax_calc = VALUES(tax_calc), " +
                "updated_at = NOW()";

        int batchSize = 1000;
        List<Reports> chunk = new ArrayList<>();

        for (int i = 0; i < reports.size(); i++) {
            chunk.add(reports.get(i));

            if ((i + 1) % batchSize == 0) {
                batch(sql, chunk);
                chunk.clear();
            }
        }

        if (!chunk.isEmpty()) {
            batch(sql, chunk);
        }
    }

    private void batch(String sql, List<Reports> chunk) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Reports report = chunk.get(i);
                ps.setString(1, report.getUser().getId());
                ps.setString(2, report.getReportType().name());
                ps.setString(3, report.getPeriodType().name());
                ps.setString(4, report.getPeriod());
                ps.setBigDecimal(5, report.getResult());

                try {
                    String jsonString = objectMapper.writeValueAsString(report.getCalc());
                    ps.setString(6, jsonString);
                } catch (JsonProcessingException e) {
                    log.error("Map to JSON 변환 실패", e);
                    ps.setString(6, "{}");
                }
            }

            @Override
            public int getBatchSize() {
                return chunk.size();
            }
        });
    }
}

