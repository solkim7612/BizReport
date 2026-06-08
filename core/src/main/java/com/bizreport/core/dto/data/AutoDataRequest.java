package com.bizreport.core.dto.data;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.data.DataType;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.user.Users;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class AutoDataRequest {
    private String id;
    private String startMon;
    private String endMon;
    private int count;

    public Data toEntity(Users user) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        DataType type = random.nextBoolean() ? DataType.SALES : DataType.PURCHASE;
        DataMethod method = DataMethod.values()[random.nextInt(DataMethod.values().length)];

        boolean isE = (method == DataMethod.INVOICE);
        boolean isMod = !isE;

        int taxOffice = random.nextInt(900) + 100;
        int taxType = random.nextInt(79) + 1;
        int serial = random.nextInt(9999) + 1;
        int validation = random.nextInt(9) + 1;
        String vendorId = String.format("%03d%02d%04d%d", taxOffice, taxType, serial, validation);

        String cardNum = null;
        if (method == DataMethod.CARD) {
            cardNum = String.format("%04d%04d%04d%04d",
                    random.nextInt(10000), random.nextInt(10000), random.nextInt(10000), random.nextInt(10000));
        }

        LocalDate start = YearMonth.parse(this.startMon).atDay(1);
        LocalDate end = YearMonth.parse(this.endMon).atEndOfMonth();

        long startEpochDay = start.toEpochDay();
        long endEpochDay = end.toEpochDay();

        long randomDay;
        if (startEpochDay == endEpochDay) {
            randomDay = startEpochDay;
        } else {
            randomDay = random.nextLong(startEpochDay, endEpochDay + 1);
        }

        LocalDate transDt = LocalDate.ofEpochDay(randomDay);

        LocalDate vatDeadline = Reports.getDeadline(ReportType.VAT, YearMonth.from(transDt));
        boolean ignoreVat = LocalDate.now().isAfter(vatDeadline);

        long randomNetValue = (random.nextInt(100) + 1) * 10000L;
        BigDecimal netValue = BigDecimal.valueOf(randomNetValue);
        BigDecimal vatValue = netValue.multiply(BigDecimal.valueOf(0.1));

        BigDecimal finalNet = ignoreVat ? netValue.add(vatValue) : netValue;
        BigDecimal finalVat = ignoreVat ? BigDecimal.ZERO : vatValue;
        BigDecimal totalPrice = finalNet.add(finalVat);

        return Data.builder()
                .user(user)
                .type(type)
                .method(method)
                .isE(isE)
                .isMod(isMod)
                .cardNum(cardNum)
                .vendorId(vendorId)
                .transDt(transDt)
                .netValue(finalNet)
                .vatValue(finalVat)
                .totalPrice(totalPrice)
                .build();
    }
}