package com.bizreport.core.entity.rate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CITRate {
    RT_6(new BigDecimal("14000000"), new BigDecimal("6"), new BigDecimal("0")),
    RT_15(new BigDecimal("50000000"), new BigDecimal("15"), new BigDecimal("1260000")),
    RT_24(new BigDecimal("88000000"), new BigDecimal("24"), new BigDecimal("5760000")),
    RT_35(new BigDecimal("150000000"), new BigDecimal("35"), new BigDecimal("15440000")),
    RT_38(new BigDecimal("300000000"), new BigDecimal("38"), new BigDecimal("19940000"));
    // 이후 세율은 성실신고의무자 이므로, 사용자 대상에서 제외

    private final BigDecimal maxBase;           // 과세표준
    private final BigDecimal rate;              // 소득세율
    private final BigDecimal deduction;         // 누진공제액

    public static CITRate getMaxBase(BigDecimal taxBase) {
        return Arrays.stream(values())
                .filter(m -> taxBase.compareTo(m.maxBase) <= 0)
                .findFirst()
                .orElse(RT_38);
    }

    public static BigDecimal calcTax(BigDecimal income) {
        BigDecimal taxBase = income.subtract(new BigDecimal("1500000"))
                .max(BigDecimal.ZERO);

        CITRate maxBase = getMaxBase(taxBase);

        BigDecimal rate = maxBase.rate.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal calculatedTax = taxBase
                .multiply(rate)
                .subtract(maxBase.deduction)
                .max(BigDecimal.ZERO);

        return calculatedTax
                .subtract(new BigDecimal("70000"))
                .max(BigDecimal.ZERO);
    }
}