package com.bizreport.core.repository.business;

import com.bizreport.core.entity.rate.RateId;
import com.bizreport.core.entity.rate.TaxRate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateRepository extends JpaRepository<TaxRate, RateId> {
    List<TaxRate> findByIdIndCdInAndIdYear(List<String> indCd, String targetYear);

    Optional<TaxRate> findFirstByIdIndCdOrderByIdYearDesc(String indCd);

    @Query("SELECT t.indNm FROM TaxRate t WHERE t.id.indCd = :indCd ORDER BY t.id.year DESC LIMIT 1")
    Optional<String> findIndustryNameByCode(@Param("indCd") String indCd);

    List<TaxRate> findByIndNmContaining(String keyword);

    @Query("SELECT t FROM TaxRate t WHERE t.id.indCd LIKE CONCAT('%', :keyword, '%')")
    List<TaxRate> findByIndCdContaining(@Param("keyword") String keyword);
}