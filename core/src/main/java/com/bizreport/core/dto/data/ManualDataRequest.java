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
    private DataType type;
    private DataMethod method;
    private String vendorId;
    private LocalDate transDt;
    private BigDecimal netValue;
    private BigDecimal vatValue;

    public BigDecimal getTotalPrice() {
        BigDecimal net = netValue != null ? netValue : BigDecimal.ZERO;
        BigDecimal vat = vatValue != null ? vatValue : BigDecimal.ZERO;
        return net.add(vat);
    }

    public Data toEntity(Users user){
        boolean isE=(method == DataMethod.INVOICE);

        return Data.builder()
                .user(user)
                .type(type)
                .method(method)
                .isE(isE)
                .isMod(!isE)
                .cardNum(null)
                .vendorId(vendorId)
                .transDt(transDt)
                .netValue(netValue)
                .vatValue(vatValue)
                .totalPrice(getTotalPrice())
                .build();
    }
}