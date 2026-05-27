package com.bizreport.core.dto.data;

import java.math.BigDecimal;

public record DataUpdateRequest(
        BigDecimal totalPrice,
        BigDecimal vatValue
) {
    public BigDecimal netValue() {
        BigDecimal total = totalPrice != null ? totalPrice : BigDecimal.ZERO;
        BigDecimal vat = vatValue != null ? vatValue : BigDecimal.ZERO;
        return total.subtract(vat);
    }
}