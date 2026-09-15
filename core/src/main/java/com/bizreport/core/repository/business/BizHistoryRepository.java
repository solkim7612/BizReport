package com.bizreport.core.repository.business;

import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BizHistoryRepository extends JpaRepository<BizHistory, Long> {
    List<BizHistory> findByUserOrderByCreatedAtDesc(Users user);
}
