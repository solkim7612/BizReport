package com.bizreport.api.domain.report;

import com.bizreport.core.dto.report.ReportCommand;
import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.report.PeriodType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.business.RateRepository;
import com.bizreport.core.repository.data.DataRepository;
import com.bizreport.core.repository.report.ReportRepository;
import com.bizreport.core.repository.business.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportService {
    private final UserRepository userRepo;
    private final DataRepository dataRepo;
    private final RateRepository rateRepo;
    private final ReportRepository reportRepo;

    private final Map<ReportType, TaxCalculator> calcs;

    public ReportService(UserRepository userRepo, DataRepository dataRepo, RateRepository rateRepo, ReportRepository reportRepo, List<TaxCalculator> calculatorList) {
        this.userRepo = userRepo;
        this.dataRepo = dataRepo;
        this.rateRepo = rateRepo;
        this.reportRepo = reportRepo;
        this.calcs = calculatorList.stream()
                .collect(Collectors.toMap(TaxCalculator::getType, Function.identity()));
    }

    @Transactional
    public ReportResponse generateAccumulated(ReportRequest request) {
        YearMonth startMon = YearMonth.parse(request.getStartMon());
        YearMonth endMon = YearMonth.parse(request.getEndMon());

        LocalDate deadline = Reports.getDeadline(request.getReportType(), endMon);
        Users user = getUser(request.getId());
        String period = request.getStartMon() + "~" + request.getEndMon();

        if (LocalDate.now().isAfter(deadline)) {
            Reports report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), request.getReportType(), PeriodType.ACCUMULATED, period)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_ALREADY_CLOSED));

            return ReportResponse.from(report);
        }

        Reports report = save(new ReportCommand(user, request.getReportType(), startMon, endMon, period, PeriodType.ACCUMULATED, deadline, request.getPrepaidTax()));
        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> calculateMonthly(Users user, YearMonth targetMon) {
        LocalDate startDt = targetMon.atDay(1);
        LocalDate endDt = targetMon.atEndOfMonth();

        List<Data> dataList = dataRepo.findAllByUserIdAndTransDtBetween(user.getId(), startDt, endDt);

        TaxRate rate = rateRepo.findFirstByIdIndCdOrderByIdYearDesc(user.getIndCd())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_INDUSTRY_CODE));

        List<ReportResponse> reports = new ArrayList<>();

        for (ReportType type : List.of(ReportType.VAT, ReportType.CIT)) {
            TaxCalculator calculator = calcs.get(type);
            TaxCalculator.Result result = calculator.calc(user, dataList, rate, BigDecimal.ZERO);

            result.calc().put("dataCount", dataList.size());

            reports.add(ReportResponse.builder()
                    .userId(user.getId())
                    .reportType(type)
                    .periodType(PeriodType.MONTHLY)
                    .period(targetMon.toString())
                    .tax(result.tax())
                    .calc(result.calc())
                    .build());
        }

        return reports;
    }

    private Reports save(ReportCommand command) {
        LocalDate startDt = command.startMon().atDay(1);
        LocalDate endDt = command.endMon().atEndOfMonth();
        String targetYear = String.valueOf(command.endMon().getYear());

        TaxCalculator calculator = calcs.get(command.reportType());
        if (calculator == null) throw new CustomException(ErrorCode.INVALID_REPORT_TYPE);

        List<Data> dataList = dataRepo.findAllByUserIdAndTransDtBetween(command.user().getId(), startDt, endDt);
        List<TaxRate> rates = rateRepo.findByIdIndCdInAndIdYear(Collections.singletonList(command.user().getIndCd()), targetYear);
        TaxRate rate = rates.stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_INDUSTRY_CODE));

        TaxCalculator.Result result = calculator.calc(command.user(), dataList, rate, command.prepaidTax());

        result.calc().put("dataCount", dataList.size());
        if (command.deadline() != null) result.calc().put("deadline", command.deadline().toString());

        Reports report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(command.user().getId(), command.reportType(), command.periodType(), command.period())
                .orElse(Reports.create(command.user(), command.reportType(), command.periodType(), command.period()));

        report.update(result.tax(), result.calc());

        return reportRepo.save(report);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(ReportRequest request) {
        boolean isMonthly = (request.getEndMon() == null || request.getEndMon().isBlank());

        PeriodType periodType = isMonthly ? PeriodType.MONTHLY : PeriodType.ACCUMULATED;
        String period = isMonthly ? request.getStartMon() : request.getStartMon() + "~" + request.getEndMon();

        YearMonth endMon = isMonthly ? YearMonth.parse(request.getStartMon()) : YearMonth.parse(request.getEndMon());
        LocalDate deadline = Reports.getDeadline(request.getReportType(), endMon);

        Reports report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(request.getId(), request.getReportType(), periodType, period)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (!LocalDate.now().isAfter(deadline)) {
            report.getCalc().put("isFinalized", false);
            report.getCalc().put("notice", "마감 전 리포트입니다. 추후 데이터 변동에 따라 세액이 달라질 수 있습니다.");
        } else {
            report.getCalc().put("isFinalized", true);
        }

        return ReportResponse.from(report);
    }

    // ==========================================
    // helper method
    // ==========================================

    private Users getUser(String id) {
        return userRepo.findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}