package com.bizreport.api.entity.report;

import com.bizreport.api.entity.global.BaseEntity;
import com.bizreport.api.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "REPORT",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_report_target",
                        columnNames = {"b_id", "report_type", "period_type", "period_target"}
                )
        }
)
public class Report extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "period_target", nullable = false)
    private String period;

    @Column(name = "tax_result", precision = 15, scale = 0)
    private BigDecimal result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_calc", columnDefinition = "json")
    private Map<String, Object> calc;

    @Builder
    private Report(User user, ReportType reportType, PeriodType periodType, String period, BigDecimal result, Map<String, Object> calc) {
        this.user = user;
        this.reportType = reportType;
        this.periodType = periodType;
        this.period = period;
        this.result = result;
        this.calc = calc;
    }

    public static Report create(User user, ReportType reportType, PeriodType periodType, String period) {
        return Report.builder()
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
}