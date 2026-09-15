package com.bizreport.core.service.report;

import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.history.RefreshHistory;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.business.RateRepository;
import com.bizreport.core.repository.business.RefreshHistoryRepository;
import com.bizreport.core.repository.business.UserRepository;
import com.bizreport.core.repository.data.DataRepository;
import com.bizreport.core.repository.report.ReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ReportService {
    private final UserRepository userRepo;
    private final DataRepository dataRepo;
    private final RateRepository rateRepo;
    private final ReportRepository reportRepo;
    private final RefreshHistoryRepository refreshHistoryRepo;
    private final Map<ReportType, TaxCalculator> calcs;

    private static final BigDecimal MIN_STANDARD_TAX = new BigDecimal("1000000");

    public ReportService(UserRepository userRepo, DataRepository dataRepo, RateRepository rateRepo,
                         ReportRepository reportRepo, RefreshHistoryRepository refreshHistoryRepo,
                         List<TaxCalculator> calculatorList) {
        this.userRepo = userRepo;
        this.dataRepo = dataRepo;
        this.rateRepo = rateRepo;
        this.reportRepo = reportRepo;
        this.refreshHistoryRepo = refreshHistoryRepo;
        this.calcs = calculatorList.stream()
                .collect(Collectors.toMap(TaxCalculator::getType, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> get(String id, ReportRequest request) {
        ReportType type = request.getReportType();

        String monthlyPeriod = request.getPeriod(PeriodType.MONTHLY);
        String accPeriod = request.getPeriod(PeriodType.ACCUMULATED);

        Reports mon = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
                id, type, PeriodType.MONTHLY, monthlyPeriod).orElse(null);

        Reports acc = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
                id, type, PeriodType.ACCUMULATED, accPeriod).orElse(null);

        if (mon == null && acc == null) {
            log.info("[REPORT] 아직 리포트 생성 기간이 아닙니다. (userId={}, type={})", id, type);
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }

        return Stream.of(mon, acc)
                .filter(Objects::nonNull)
                .map(this::finalizeReport)
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional
    public void update(String id, ReportRequest request) {
        Users user = getUser(id);
        String monthlyPeriod = request.getPeriod(PeriodType.MONTHLY);
        String accPeriod = request.getPeriod(PeriodType.ACCUMULATED);

        Reports monthly = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
                        id,
                        request.getReportType(),
                        PeriodType.MONTHLY,
                        monthlyPeriod)
                .orElse(null);

        Reports accumulated = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
                        id,
                        request.getReportType(),
                        PeriodType.ACCUMULATED,
                        accPeriod)
                .orElse(null);

        if ((monthly != null && monthly.isClosed()) || (accumulated != null && accumulated.isClosed())) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        List<ReportResponse> responses = create(id, request);
        ReportResponse mon = responses.stream().filter(r -> r.getPeriodType() == PeriodType.MONTHLY).findFirst().orElseThrow();
        ReportResponse acc = responses.stream().filter(r -> r.getPeriodType() == PeriodType.ACCUMULATED).findFirst().orElseThrow();

        if (monthly != null) {
            monthly.update(mon.getTax(), mon.getCalc());
        } else {
            monthly = Reports.builder()
                    .user(user)
                    .reportType(request.getReportType())
                    .periodType(PeriodType.MONTHLY)
                    .period(monthlyPeriod)
                    .result(mon.getTax())
                    .calc(mon.getCalc())
                    .build();
        }

        if (accumulated != null) {
            accumulated.update(acc.getTax(), acc.getCalc());
        } else {
            accumulated = Reports.builder()
                    .user(user)
                    .reportType(request.getReportType())
                    .periodType(PeriodType.ACCUMULATED)
                    .period(accPeriod)
                    .result(acc.getTax())
                    .calc(acc.getCalc())
                    .build();
        }

        reportRepo.save(monthly);
        reportRepo.save(accumulated);
    }

    @Transactional
    public void refresh(String id, ReportRequest request) {
        Users user = getUser(id);

        int amount = 1;
        user.use();

        refreshHistoryRepo.save(new RefreshHistory(user, "USE", amount, user.getRefreshCount()));
        update(id, request);
        log.info("[REPORT] B_NO {} 리포트 즉시 갱신 완료. 잔여 횟수: {}", user.getId(), user.getRefreshCount());
    }

    @Transactional(readOnly = true)
    public BigDecimal getPrepaidTax(String id, ReportRequest request) {
        ReportType reportType = request.getReportType();
        YearMonth targetMon = request.getEndMon();

        YearMonth prevTargetMon;
        if (reportType == ReportType.VAT) {
            int year = targetMon.getYear();
            int month = targetMon.getMonthValue();

            if (month <= 6) {
                prevTargetMon = YearMonth.of(year - 1, 12);
            } else {
                prevTargetMon = YearMonth.of(year, 6);
            }
        } else {
            int year = targetMon.getYear();
            prevTargetMon = YearMonth.of(year - 1, 12);
        }

        ReportRequest prevRequest = new ReportRequest(reportType, prevTargetMon.toString(), BigDecimal.ZERO);
        String prevPeriod = prevRequest.getPeriod(PeriodType.ACCUMULATED);

        return reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(
                        id, reportType, PeriodType.ACCUMULATED, prevPeriod)
                .map(report -> {
                    Object rawBeforeTax = report.getCalc().get("beforeTax");
                    if (rawBeforeTax == null) {
                        return BigDecimal.ZERO;
                    }

                    BigDecimal beforeTax;
                    if (rawBeforeTax instanceof BigDecimal) {
                        beforeTax = (BigDecimal) rawBeforeTax;
                    } else {
                        beforeTax = new BigDecimal(rawBeforeTax.toString());
                    }

                    if (beforeTax.compareTo(MIN_STANDARD_TAX) < 0) {
                        return BigDecimal.ZERO;
                    }

                    return beforeTax.multiply(new BigDecimal("0.5")).setScale(-1, RoundingMode.DOWN);
                })
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> create(String id, ReportRequest request) {
        ReportResponse mon = generate(id, request, PeriodType.MONTHLY);
        ReportResponse acc = generate(id, request, PeriodType.ACCUMULATED);

        return List.of(mon, acc);
    }

    // ==========================================
    // helper method
    // ==========================================

    private Users getUser(String id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }


    private Reports finalizeReport(Reports report) {
        LocalDate deadline = Reports.getDeadline(report.getReportType(), YearMonth.parse(report.getPeriod().split("~")[0]));
        boolean isFinalized = LocalDate.now().isAfter(deadline);

        report.getCalc().put("isFinalized", isFinalized);
        if (!isFinalized) {
            report.getCalc().put("notice", "마감 전 리포트입니다.");
        }
        return report;
    }

    private ReportResponse generate(String id, ReportRequest request, PeriodType periodType) {
        Users user = getUser(id);
        LocalDate startDt = (periodType == PeriodType.MONTHLY) ? request.getEndMon().atDay(1) : request.getStartMon().atDay(1);
        LocalDate endDt = request.getEndMon().atEndOfMonth();

        List<Data> dataList = dataRepo.findAllByUserIdAndTransDtBetween(id, startDt, endDt);

        TaxRate rate = rateRepo.findFirstByIdIndCdOrderByIdYearDesc(user.getIndCd())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_INDUSTRY_CODE));

        TaxCalculator calculator = calcs.get(request.getReportType());
        if (calculator == null) {
            log.error("[REPORT] 지원하지 않는 리포트 타입입니다: {}", request.getReportType());
            throw new CustomException(ErrorCode.INVALID_REPORT_TYPE);
        }

        TaxCalculator.Result result = calculator.calc(
                user,
                dataList,
                rate,
                request.getPrepaidTax()
        );

        result.calc().put("dataCount", dataList.size());

        ReportType type = request.getReportType();
        String period = request.getPeriod(periodType);

        return ReportResponse.of(id, type, periodType, period, result.tax(), result.calc());
    }
}