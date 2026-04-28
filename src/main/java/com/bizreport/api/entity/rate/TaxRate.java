package com.bizreport.api.entity.rate;

import com.bizreport.api.entity.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "TAX_RATE")
public class TaxRate extends BaseEntity {
    @EmbeddedId
    private RateId id;

    @Column(name = "vat_rate", nullable = false, precision = 2, scale = 0)
    private BigDecimal vatRate;

    @Column(name = "exp_rate", nullable = false, precision = 4, scale = 1)
    private BigDecimal expRate;

    @Builder
    public TaxRate(RateId id, BigDecimal vatRate, BigDecimal expRate) {
        this.id = id;
        this.vatRate = vatRate;
        this.expRate = expRate;
    }
}