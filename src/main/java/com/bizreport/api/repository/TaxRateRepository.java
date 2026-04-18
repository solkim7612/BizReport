package com.bizreport.api.repository;

import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRateRepository extends JpaRepository<TaxRate, RateId> {
}
