package com.bizreport.core.dto.report;

import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.Users;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record ReportCommand(
        Users user, ReportType reportType, YearMonth startMon, YearMonth endMon,
        String period, PeriodType periodType, LocalDate deadline, BigDecimal prepaidTax
) {}