package com.bizreport.api.repository;

import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, RateId> {

    Optional<TaxRate> findFirstByIndCdOrderByYearDesc(String indCd);
}