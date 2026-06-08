package com.bizreport.core.dto.data;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.data.DataType;
import com.bizreport.core.entity.user.Users;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ManualDataRequest {
    private String id;
    private String vendorId;
    private LocalDate transDt;
    private BigDecimal totalPrice;
    private BigDecimal vatValue;

    public BigDecimal getNetValue() {
        BigDecimal total = totalPrice != null ? totalPrice : BigDecimal.ZERO;
        BigDecimal vat = vatValue != null ? vatValue : BigDecimal.ZERO;
        return total.subtract(vat);
    }

    public Data toEntity(Users user, boolean ignoreVat) {
        BigDecimal parseVat = ignoreVat ? BigDecimal.ZERO : vatValue;

        return Data.builder()
                .user(user)
                .type(DataType.PURCHASE)
                .method(DataMethod.CARD)
                .isE(false)
                .isMod(true)
                .cardNum(null)
                .vendorId(vendorId)
                .transDt(transDt)
                .netValue(getNetValue())
                .vatValue(parseVat)
                .totalPrice(totalPrice)
                .build();
    }
}