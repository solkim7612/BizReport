package com.bizreport.core.dto.data;

import com.bizreport.core.entity.data.Data;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DataResponse {
    private Long id;
    private String type;
    private String method;
    private boolean isMod;
    private String vendorId;
    private LocalDate transDt;
    private BigDecimal netValue;
    private BigDecimal vatValue;
    private BigDecimal totalPrice;

    public static DataResponse from(Data data) {
        return DataResponse.builder()
                .id(data.getId())
                .type(data.getType().name())
                .method(data.getMethod().name())
                .isMod(data.isMod())
                .vendorId(data.getVendorId())
                .transDt(data.getTransDt())
                .netValue(data.getNetValue())
                .vatValue(data.getVatValue())
                .totalPrice(data.getTotalPrice())
                .build();
    }
}