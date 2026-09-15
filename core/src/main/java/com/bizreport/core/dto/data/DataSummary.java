package com.bizreport.core.dto.data;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DataSummary {
    private long count;
    private BigDecimal totalNetValue;
    private BigDecimal totalVatValue;
    private BigDecimal totalPrice;
}