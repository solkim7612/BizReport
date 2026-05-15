package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.Users;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ReportCommand(
        Users user,
        ReportType reportType,
        PeriodType periodType,
        YearMonth startMon,
        YearMonth endMon,
        BigDecimal prepaidTax
) {
    public String period() {
        return (periodType == PeriodType.MONTHLY) ? startMon.toString() : startMon + "~" + endMon;
    }

    public BigDecimal getPrepaidTax() {
        return prepaidTax != null ? prepaidTax : BigDecimal.ZERO;
    }
}