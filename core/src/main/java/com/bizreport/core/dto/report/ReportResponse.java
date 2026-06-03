package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.report.Reports;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class ReportResponse {
    private Long id;
    private String userId;
    private ReportType reportType;
    private PeriodType periodType;
    private String period;
    private BigDecimal tax;
    private Map<String, Object> calc;

    public static ReportResponse from(Reports report) {
        return ReportResponse.builder()
                .id(report.getId())
                .userId(report.getUser().getId())
                .reportType(report.getReportType())
                .periodType(report.getPeriodType())
                .period(report.getPeriod())
                .tax(report.getResult())
                .calc(report.getCalc())
                .build();
    }

    public static ReportResponse of(String userId, ReportType reportType, PeriodType periodType,
                                    String period, BigDecimal tax, Map<String, Object> calc) {
        return ReportResponse.builder()
                .userId(userId)
                .reportType(reportType)
                .periodType(periodType)
                .period(period)
                .tax(tax)
                .calc(calc)
                .build();
    }
}