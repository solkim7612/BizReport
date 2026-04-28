package com.bizreport.api.entity.rate;

import com.bizreport.api.entity.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "TAX_RATE")
public class TaxRate extends BaseEntity {
    @EmbeddedId
    private RateId id;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "exp_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal expRate;
}