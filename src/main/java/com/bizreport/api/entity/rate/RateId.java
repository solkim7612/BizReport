package com.bizreport.api.entity.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class RateId implements Serializable {
    @Column(name = "ind_cd")
    private String indCd;

    @Column(name = "target_year")
    private int year;

    public RateId(String indCd, int year) {
        this.indCd = indCd;
        this.year = year;
    }
}
