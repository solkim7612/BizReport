package com.bizreport.api.repository;

import com.bizreport.api.entity.history.BizHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository extends JpaRepository<BizHistory, Long> {
}
