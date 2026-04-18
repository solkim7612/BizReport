package com.bizreport.api.dto.admin;

import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import com.bizreport.api.entity.rate.VatRate;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ImportFileRequest {
    private String targetYear;
    private String indCode;
    private String indName;
    private String category1;
    private String category2;
    private String category3;
    private String content;
    private String expRate;
    private String overRate;
    private String baseRate;

    public TaxRate toEntity(){
        RateId id = new RateId(this.getIndCode(), Integer.parseInt(this.getTargetYear()));

        BigDecimal expRate = (this.getExpRate() == null || this.getExpRate().isEmpty())
                ? BigDecimal.ZERO
                : new BigDecimal(this.getExpRate());

        String category = this.getIndCode() + " " + this.getCategory1() + " " + this.getCategory2();
        BigDecimal vatRate = VatRate.findRateByCategory(category);

        return TaxRate.builder()
                .id(id)
                .expRate(expRate)
                .vatRate(vatRate)
                .build();
    }
}
