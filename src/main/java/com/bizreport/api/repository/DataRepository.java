package com.bizreport.api.repository;

import com.bizreport.api.entity.data.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DataRepository extends JpaRepository<Data, Long> {

    List<Data> findAllByUserId(String userId);

    List<Data> findAllByUserIdAndTransDateBetween(String userId, LocalDate startDate, LocalDate endDate);
}