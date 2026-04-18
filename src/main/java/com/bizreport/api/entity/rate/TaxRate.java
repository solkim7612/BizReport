package com.bizreport.api.entity.rate;

import com.bizreport.api.dto.admin.ImportFileRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "TAX_RATE")
public class TaxRate {
    @EmbeddedId
    private RateId id;

    @Column(name = "vat_rt", precision = 2, scale = 0)
    private BigDecimal vatRate;

    @Column(name = "exp_rt", precision = 3, scale = 1)
    private BigDecimal expRate;

    @Builder
    private TaxRate(RateId id, BigDecimal vatRate, BigDecimal expRate) {
        this.id = id;
        this.vatRate = vatRate;
        this.expRate = expRate;
    }
}
