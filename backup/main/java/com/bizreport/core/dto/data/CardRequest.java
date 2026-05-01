package com.bizreport.core.dto.data;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.data.DataType;
import com.bizreport.core.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CardRequest {
    private String cardNum;
    private String transDt;
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

    public void validPrice(){
        BigDecimal totalPrice=parsedNetValue().add(parsedVatValue());
        if(totalPrice.compareTo(this.totalPrice)!=0){
            throw new IllegalArgumentException("공급가액과 부가세의 합이 총 결제금액과 일치하지 않습니다. (CardNum: " + cardNum + ")");
        }
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
                .transDt(LocalDate.parse(this.transDt))
                .netValue(parsedNetValue())
                .vatValue(parsedVatValue())
                .totalPrice(this.totalPrice)
                .build();
    }
}