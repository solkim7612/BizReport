package com.bizreport.api.domain.report;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataType;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.user.TaxType;
import com.bizreport.core.entity.user.User;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VatCalculator implements TaxCalculator {

    @Override
    public ReportType getType() {
        return ReportType.VAT;
    }

    @Override
    public Result calc(User user, List<Data> dataList, TaxRate rate, BigDecimal prepaidTax) {
        if (user.getIndCd() == null) throw new CustomException(ErrorCode.MISSING_INDUSTRY_CODE);

        boolean isGeneral = (user.getTaxType() == TaxType.GENERAL);

        BigDecimal sales = isGeneral
                ? sumBy(dataList, DataType.SALES, Data::getNetValue)
                : sumBy(dataList, DataType.SALES, Data::getTotalPrice);

        BigDecimal salesTax = isGeneral
                ? sumBy(dataList, DataType.SALES, Data::getVatValue)
                : sales.multiply(rate.getVatRt().getRate()).multiply(BigDecimal.valueOf(0.1)); // Enum의 실제 값을 가져온다고 가정

        BigDecimal purchases = isGeneral
                ? sumBy(dataList, DataType.PURCHASE, Data::getNetValue)
                : dataList.stream()
                .filter(d -> d.getType() == DataType.PURCHASE)
                .filter(d -> d.getVatValue() != null && d.getVatValue().compareTo(BigDecimal.ZERO) > 0)
                .map(Data::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal purchaseTax = isGeneral
                ? sumBy(dataList, DataType.PURCHASE, Data::getVatValue)
                : purchases.multiply(rate.getVatRt().getRate()).multiply(BigDecimal.valueOf(0.005));

        BigDecimal beforeTax = salesTax.subtract(purchaseTax).max(BigDecimal.ZERO);

        BigDecimal tax = beforeTax.subtract(prepaidTax);
        boolean isRefund = tax.compareTo(BigDecimal.ZERO) < 0;

        BigDecimal pay = isRefund ? BigDecimal.ZERO : tax;
        BigDecimal refund = isRefund ? tax.abs() : BigDecimal.ZERO;

        Map<String, Object> calc = new HashMap<>();
        calc.put("isGeneral", isGeneral);
        calc.put("vatRt", isGeneral ? null : rate.getVatRt());
        calc.put("sales", sales);
        calc.put("salesTax", salesTax);
        calc.put("purchases", purchases);
        calc.put("purchaseTax", purchaseTax);
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