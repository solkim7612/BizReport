package com.bizreport.core.entity.rate;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "TAX_RATE",
        indexes = {
                @Index(name = "idx_tax_rate_year", columnList = "target_year")
        }
)
public class TaxRate {
    @EmbeddedId
    private RateId id;

    @Column(name = "ind_nm")
    private String indNm;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_rt", nullable = false)
    private VatRate vatRt;

    @Column(name = "exp_rt", nullable = false, precision = 3, scale = 1)
    private BigDecimal expRt;

    @Builder
    public TaxRate(RateId id, String indNm, VatRate vatRt, BigDecimal expRt) {
        this.id = id;
        this.indNm = indNm;
        this.vatRt = vatRt;
        this.expRt = expRt;
    }
}