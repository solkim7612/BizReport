package com.bizreport.api.domian.report;

import com.bizreport.api.dto.report.ReportRequest;
import com.bizreport.api.entity.global.TaxType;
import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.data.DataType;
import com.bizreport.api.entity.rate.CITTax;
import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import com.bizreport.api.entity.report.PeriodType;
import com.bizreport.api.entity.report.ReportType;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.entity.report.Report;
import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import com.bizreport.api.repository.DataRepository;
import com.bizreport.api.repository.ReportRepository;
import com.bizreport.api.repository.TaxRateRepository;
import com.bizreport.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CITCalcService {
    private final UserRepository userRepo;
    private final DataRepository dataRepo;
    private final ReportRepository reportRepo;
    private final TaxRateRepository taxRateRepo;

    private record Result(BigDecimal amount, Map<String, Object> snapshot) {}

    @Transactional
    public Report generateAccumulated(ReportRequest request) {
        User user = getUser(request.getId());
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        String period = request.getStartDate() + "-" + request.getEndDate();

        LocalDate deadline = LocalDate.of(endDate.getYear() + 1, 5, 31);

        if (LocalDate.now().isAfter(deadline)) {
            return reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), ReportType.CIT, PeriodType.ACCUMULATED, period)
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORT_ALREADY_CLOSED));
        }

        return save(user, startDate, endDate, period, PeriodType.ACCUMULATED, deadline);
    }

    @Transactional
    public Report generateMonthly(String id, YearMonth targetMonth) {
        User user = getUser(id);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        return save(user, startDate, endDate, targetMonth.toString(), PeriodType.MONTHLY, null);
    }

    private Report save(User user, LocalDate startDate, LocalDate endDate, String period, PeriodType periodType, LocalDate deadline) {
        List<Data> dataList = dataRepo.findAllByUserIdAndTransDateBetween(user.getId(), startDate, endDate);

        Result result = calcCit(user, dataList, String.valueOf(endDate.getYear()));

        result.snapshot().put("dataCount", dataList.size());
        if (deadline != null) result.snapshot().put("deadline", deadline.toString());

        Report report = reportRepo.findByUserIdAndReportTypeAndPeriodTypeAndPeriod(user.getId(), ReportType.CIT, periodType, period)
                .orElse(Report.create(user, ReportType.CIT, periodType, period));

        report.update(result.amount(), result.snapshot());
        return reportRepo.save(report);
    }

    private Result calcCit(User user, List<Data> dataList, String year) {
        if (user.getIndCd() == null) throw new CustomException(ErrorCode.MISSING_INDUSTRY_CODE);

        Map<String, Object> snapshot = new HashMap<>();
        TaxRate rate = taxRateRepo.findById(new RateId(user.getIndCd(), year))
                .orElseGet(() -> taxRateRepo.findFirstByIndCdOrderByYearDesc(user.getIndCd())
                        .orElseThrow(() -> new CustomException(ErrorCode.MISSING_INDUSTRY_CODE)));

        BigDecimal totalSales = (user.getTaxType() == TaxType.GENERAL)
                ? sumBy(dataList, DataType.SALES, Data::getNetValue)
                : sumBy(dataList, DataType.SALES, Data::getTotalPrice);

        BigDecimal estimatedExpense = totalSales.multiply(rate.getExpRate());
        BigDecimal profit = totalSales.subtract(estimatedExpense).max(BigDecimal.ZERO);

        // ✅ 누진세율 Enum 적용
        BigDecimal finalTax = CITTax.calculateTax(profit);

        snapshot.put("taxType", "CIT_SIMPLE_EXPENSE");
        snapshot.put("totalSales", totalSales);
        snapshot.put("profit", profit);
        snapshot.put("appliedCitRate", CITTax.getAppliedRate(profit));

        return new Result(finalTax, snapshot);
    }

    private User getUser(String id) {
        return userRepo.findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal sumBy(List<Data> dataList, DataType type, java.util.function.Function<Data, BigDecimal> mapper) {
        return dataList.stream().filter(d -> d.getType() == type).map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}