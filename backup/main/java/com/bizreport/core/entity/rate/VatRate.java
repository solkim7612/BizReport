package com.bizreport.core.entity.rate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum VatRate {
    RT_15(new BigDecimal("15")),
    RT_20(new BigDecimal("20")),
    RT_25(new BigDecimal("25")),
    RT_40(new BigDecimal("40")),
    RT_30(new BigDecimal("30"));

    private final BigDecimal rate;         // 간이과세자 부가세율
}
