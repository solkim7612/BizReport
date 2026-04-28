package com.bizreport.api.dto.data;

import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.data.DataMethod;
import com.bizreport.api.entity.data.DataType;
import com.bizreport.api.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CardRequest {
    private String cardNum;
    private String transDate;
    private String venderId;
    private String netValue;
    private String vatValue;
    private BigDecimal totalPrice;

    public BigDecimal parsedNetValue() {
        return (netValue == null || netValue.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(netValue.replaceAll(",", ""));
    }

    public BigDecimal parsedVatValue() {
        return (vatValue == null || vatValue.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(vatValue.replaceAll(",", ""));
    }

    public Data toEntity(User user) {
        return Data.builder()
                .user(user)
                .type(DataType.PURCHASE)
                .method(DataMethod.CARD)
                .isE(false)
                .isMod(true)
                .cardNum(this.cardNum)
                .vendorId(this.venderId)
                .transDate(LocalDate.parse(this.transDate))
                .netValue(parsedNetValue())
                .vatValue(parsedVatValue())
                .totalPrice(this.totalPrice)
                .build();
    }
}