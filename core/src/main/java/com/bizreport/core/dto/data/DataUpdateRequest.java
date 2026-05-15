package com.bizreport.core.dto.data;

import java.math.BigDecimal;

public record DataUpdateRequest(
        BigDecimal netValue,
        BigDecimal vatValue
) {}