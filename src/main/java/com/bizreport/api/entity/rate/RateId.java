package com.bizreport.api.entity.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RateId implements Serializable {
    @Column(name = "ind_cd")
    private String indCd;

    @Column(name = "year")
    private String year;
}