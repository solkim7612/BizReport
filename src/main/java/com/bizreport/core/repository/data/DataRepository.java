package com.bizreport.core.repository.data;

import com.bizreport.core.entity.data.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DataRepository extends JpaRepository<Data, Long> {
    List<Data> findAllByUserIdAndTransDateBetween(String id, LocalDate startDt, LocalDate endDt);
    List<Data> findAllByUserIdInAndTransDateBetween(List<String> ids, LocalDate startDt, LocalDate endDt);
}