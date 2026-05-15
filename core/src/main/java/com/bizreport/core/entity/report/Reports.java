package com.bizreport.core.entity.report;

import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.entity.global.BaseEntity;
import com.bizreport.core.entity.user.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "REPORTS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_report_target",
                        columnNames = {"b_id", "report_type", "period_type", "period_target"}
                )
        }
)
public class Reports extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "period_target", nullable = false)
    private String period;

    @Column(name = "tax_result")
    private BigDecimal result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_calc", columnDefinition = "json")
    private Map<String, Object> calc;

    @Builder
    private Reports(Users user, ReportType reportType, PeriodType periodType, String period, BigDecimal result, Map<String, Object> calc) {
        this.user = user;
        this.reportType = reportType;
        this.periodType = periodType;
        this.period = period;
        this.result = result;
        this.calc = calc;
    }

    public static Reports create(Users user, ReportType reportType, PeriodType periodType, String period) {
        return Reports.builder()
                .user(user)
                .reportType(reportType)
                .periodType(periodType)
                .period(period)
                .build();
    }

    public void update(BigDecimal result, Map<String, Object> calc) {
        this.result = result;
        this.calc = calc;
    }

    public static LocalDate getDeadline(ReportType reportType, YearMonth endMonth) {
        if (reportType == ReportType.VAT) {
            return (endMonth.getMonthValue() <= 6)
                    ? LocalDate.of(endMonth.getYear(), 7, 25)
                    : LocalDate.of(endMonth.getYear() + 1, 1, 25);
        } else if (reportType == ReportType.CIT) {
            return LocalDate.of(endMonth.getYear() + 1, 5, 31);
        }
        throw new CustomException(ErrorCode.INVALID_REPORT_TYPE);
    }

    public static LocalDate getDeadline(ReportType reportType, YearMonth startMon, YearMonth endMon) {
        LocalDate start = getDeadline(reportType, startMon);
        LocalDate end = getDeadline(reportType, endMon);

        if (!start.equals(end)) {
            throw new CustomException(ErrorCode.INVALID_REPORT_DEADLINE);
        }

        return end;
    }
}