package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.ReportType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Setter
public class ReportRequest {
    private ReportType reportType;
    private String endMon;
    private BigDecimal prepaidTax;

    public ReportRequest(ReportType reportType, String endMon, BigDecimal prepaidTax) {
        this.reportType = reportType;
        this.endMon = endMon;
        this.prepaidTax = prepaidTax;
    }

    public YearMonth getEndMon() {
        return (endMon != null && !endMon.isBlank()) ? YearMonth.parse(endMon) : null;
    }

    public YearMonth getStartMon() {
        YearMonth targetMon = getEndMon();

        if (reportType == ReportType.VAT) {
            int end = targetMon.getMonthValue();
            int start = (end <= 6) ? 1 : 7;
            return YearMonth.of(targetMon.getYear(), start);
        }
        return YearMonth.of(targetMon.getYear(), 1);
    }

    public BigDecimal getPrepaidTax() {
        return prepaidTax != null ? prepaidTax : BigDecimal.ZERO;
    }

    public String getPeriod(PeriodType periodType) {
        YearMonth targetMon = getEndMon();
        if (periodType == PeriodType.MONTHLY) {
            return targetMon.toString();
        }
        return getStartMon().toString() + "~" + targetMon.toString();
    }
}