package com.bizreport.core.repository.data;

import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.data.DataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DataRepository extends JpaRepository<Data, Long> {
    List<Data> findAllByUserIdAndTransDtBetween(String id, LocalDate startDt, LocalDate endDt);

    boolean existsByUserIdAndMethodAndCardNumAndTransDtBetween(String id, DataMethod method, String cardNum, LocalDate startDt, LocalDate endDt);

    @Query("SELECT d FROM Data d WHERE d.user.id = :userId AND d.transDt >= :startDt AND d.transDt <= :endDt " +
            "AND (:type IS NULL OR d.type = :type) " +
            "AND (:method IS NULL OR d.method = :method)")
    List<Data> findFilteredData(@Param("userId") String userId,
                                @Param("startDt") LocalDate startDt,
                                @Param("endDt") LocalDate endDt,
                                @Param("type") DataType type,
                                @Param("method") DataMethod method);
}