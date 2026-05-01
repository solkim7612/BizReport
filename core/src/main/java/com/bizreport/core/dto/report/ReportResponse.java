package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.Report;
import com.bizreport.core.entity.report.ReportType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class ReportResponse {
    private Long id;
    private ReportType reportType;
    private PeriodType periodType;
    private String period;
    private BigDecimal tax;
    private Map<String, Object> calc;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .periodType(report.getPeriodType())
                .period(report.getPeriod())
                .tax(report.getResult())
                .calc(report.getCalc())
                .build();
    }
}