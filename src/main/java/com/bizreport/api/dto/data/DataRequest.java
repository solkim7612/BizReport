package com.bizreport.api.dto.data;

import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.data.DataMethod;
import com.bizreport.api.entity.data.DataType;
import com.bizreport.api.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class DataRequest {
    private String id;
    private int targetYear;
    private int count;

    public Data toEntity(User user) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        DataType type = random.nextBoolean() ? DataType.SALES : DataType.PURCHASE;
        DataMethod method = DataMethod.values()[random.nextInt(DataMethod.values().length)];

        boolean isE = random.nextBoolean();
        boolean isMod = isE && random.nextInt(10) == 0;

        int taxOfficeCode = random.nextInt(900) + 100;
        int typeCode = random.nextInt(79) + 1;
        int serialNumber = random.nextInt(9999) + 1;
        int validationCode = random.nextInt(9) + 1;
        String vendorId = String.format("%03d%02d%04d%d", taxOfficeCode, typeCode, serialNumber, validationCode);

        String cardNum = null;
        if (method == DataMethod.CARD) {
            cardNum = String.format("%04d-%04d-%04d-%04d",
                    random.nextInt(10000), random.nextInt(10000), random.nextInt(10000), random.nextInt(10000));
        }

        int randomMonth = random.nextInt(12) + 1;
        int randomDay = random.nextInt(28) + 1;
        LocalDate transDate = LocalDate.of(this.targetYear, randomMonth, randomDay);

        long randomNetValue = (random.nextInt(100) + 1) * 10000L;
        BigDecimal netValue = BigDecimal.valueOf(randomNetValue);
        BigDecimal vatValue = netValue.multiply(BigDecimal.valueOf(0.1));
        BigDecimal totalPrice = netValue.add(vatValue);

        return Data.builder()
                .user(user)
                .type(type)
                .method(method)
                .isE(isE)
                .isMod(isMod)
                .cardNum(cardNum)
                .vendorId(vendorId)
                .transDate(transDate)
                .netValue(netValue)
                .vatValue(vatValue)
                .totalPrice(totalPrice)
                .build();
    }
}