package com.bizreport.core.dto.business;

import com.bizreport.core.entity.rate.RateId;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.rate.VatRate;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RateRequest {
    private String year;
    private String indCd;
    private String indNm;
    private String category1;
    private String category2;
    private String category3;
    private String msg;
    private BigDecimal expRt;
    private BigDecimal overExpRt;
    private BigDecimal stndExpRt;

    public TaxRate toEntity(){
        return TaxRate.builder()
                .id(new RateId(this.indCd, this.year))
                .indNm(this.indNm)
                .vatRt(categorize())
                .expRt(this.expRt)
                .build();
    }

    private VatRate categorize(){
        String c1 = (category1 == null) ? "" : category1;
        String c2 = (category2 == null) ? "" : category2;
        String c3 = (category3 == null) ? "" : category3;

        String category = (c1 + c2 + c3).replaceAll("\\s", "");

        if (containsKey(category, "소매업", "재생용", "음식점업")) return VatRate.RT_15;
        if (containsKey(category, "제조업", "농업", "임업", "어업", "소화물")) return VatRate.RT_20;
        if (containsKey(category, "숙박업")) return VatRate.RT_25;
        if (containsKey(category, "금융", "보험", "기술서비스업", "사업시설", "부동산")) return VatRate.RT_40;
        if (category.contains("기술서비스업") && category.contains("인물")) return VatRate.RT_30;

        return VatRate.RT_30;
    }

    private boolean containsKey(String target, String... keywords) {
        for (String key : keywords) {
            if (target.contains(key)) return true;
        }
        return false;
    }
}
