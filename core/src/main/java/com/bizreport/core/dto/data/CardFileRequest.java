package com.bizreport.core.dto.data;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.data.DataType;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.entity.user.Users;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CardFileRequest {
    private String transDt;
    private String vendorId;
    private String netValue;
    private String vatValue;
    private BigDecimal totalPrice;

    public BigDecimal parsedNetValue() {
        return (netValue == null || netValue.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(netValue.replaceAll(",", ""));
    }

    public BigDecimal parsedVatValue() {
        return (vatValue == null || vatValue.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(vatValue.replaceAll(",", ""));
    }

    public void validPrice() {
        BigDecimal totalPrice = parsedNetValue().add(parsedVatValue());
        if (totalPrice.compareTo(this.totalPrice) != 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public Data toEntity(Users user, String cardNum, boolean ignoreVat) {
        String parseCardNum = cardNum.replaceAll("-", "");
        String parseVendorId = this.vendorId.replaceAll("-", "");

        BigDecimal parseNet = ignoreVat ? this.totalPrice : parsedNetValue();
        BigDecimal parseVat = ignoreVat ? BigDecimal.ZERO : parsedVatValue();

        return Data.builder()
                .user(user)
                .type(DataType.PURCHASE)
                .method(DataMethod.CARD)
                .isE(false)
                .cardNum(parseCardNum)
                .vendorId(parseVendorId)
                .transDt(LocalDate.parse(this.transDt))
                .netValue(parseNet)
                .vatValue(parseVat)
                .totalPrice(this.totalPrice)
                .build();
    }
}