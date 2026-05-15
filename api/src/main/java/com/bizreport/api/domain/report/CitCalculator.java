package com.bizreport.api.domain.report;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataType;
import com.bizreport.core.entity.rate.CITRate;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CitCalculator implements TaxCalculator {

    @Override
    public ReportType getType() {
        return ReportType.CIT;
    }

    @Override
    public Result calc(Users user, List<Data> dataList, TaxRate rate, BigDecimal prepaidTax) {
        boolean isGeneral = (user.getTaxType() == TaxType.GENERAL);

        BigDecimal expRt = rate.getExpRt().divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);

        BigDecimal totalSales = isGeneral
                ? sumBy(dataList, DataType.SALES, Data::getNetValue)
                : sumBy(dataList, DataType.SALES, Data::getTotalPrice);

        BigDecimal actPurchase = isGeneral
                ? sumBy(dataList, DataType.PURCHASE, d -> (d.getNetValue() == null || d.getNetValue().compareTo(BigDecimal.ZERO) == 0) ? d.getTotalPrice() : d.getNetValue())
                : sumBy(dataList, DataType.PURCHASE, Data::getTotalPrice);

        BigDecimal expPurchase = totalSales.multiply(expRt);

        BigDecimal totalPurchase = actPurchase.max(expPurchase);

        BigDecimal profit = totalSales.subtract(totalPurchase).max(BigDecimal.ZERO);
        BigDecimal beforeTax = CITRate.calcTax(profit).max(BigDecimal.ZERO);

        BigDecimal tax = beforeTax.subtract(prepaidTax);
        boolean isRefund = tax.compareTo(BigDecimal.ZERO) < 0;

        BigDecimal pay = isRefund ? BigDecimal.ZERO : tax.setScale(-1, RoundingMode.DOWN);
        BigDecimal refund = isRefund ? tax.abs().setScale(-1, RoundingMode.DOWN) : BigDecimal.ZERO;

        Map<String, Object> calc = new HashMap<>();
        calc.put("isGeneral", isGeneral);
        calc.put("totalSales", totalSales);
        calc.put("actPurchase", actPurchase);
        calc.put("expPurchase", expPurchase);
        calc.put("totalPurchase", totalPurchase);
        calc.put("profit", profit);
        calc.put("beforeTax", beforeTax);
        calc.put("prepaidTax", prepaidTax);
        calc.put("isRefund", isRefund);
        calc.put("refund", refund);

        return new Result(pay, calc);
    }

    private BigDecimal sumBy(List<Data> dataList, DataType type, java.util.function.Function<Data, BigDecimal> mapper) {
        return dataList.stream().filter(d -> d.getType() == type).map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}