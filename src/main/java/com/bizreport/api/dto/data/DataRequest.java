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
    private String id;         // 타겟 사업자등록번호
    private int targetYear;    // 생성할 연도
    private int count;         // 생성할 데이터 개수

    public Data toEntity(User user) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        DataType type = random.nextBoolean() ? DataType.SALES : DataType.PURCHASE;
        DataMethod method = DataMethod.values()[random.nextInt(DataMethod.values().length)];

        boolean isE = random.nextBoolean();
        boolean isMod = isE;

        // 세무서(3) + 법인/개인구분(2) + 일련번호(4) + 검증번호(1)
        int taxOfficeCode = random.nextInt(900) + 100;
        int typeCode = random.nextInt(79) + 1;
        int serialNumber = random.nextInt(9999) + 1;
        int validationCode = random.nextInt(9) + 1;

        String vendorId = String.format("%03d%02d%04d%d", taxOfficeCode, typeCode, serialNumber, validationCode);

        // 랜덤 날짜 생성
        int randomMonth = random.nextInt(12) + 1;
        int randomDay = random.nextInt(28) + 1;
        LocalDate transDate = LocalDate.of(this.targetYear, randomMonth, randomDay);

        // 랜덤 금액 생성 및 부가세/합계 계산
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
                .vendorId(vendorId)
                .transDate(transDate)
                .netValue(netValue)
                .vatValue(vatValue)
                .totalPrice(totalPrice)
                .build();
    }
}