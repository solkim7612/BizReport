package com.bizreport.core.repository.business;

import com.bizreport.core.entity.history.BizHistory;
import com.bizreport.core.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends JpaRepository<BizHistory, Long> {
    BizHistory findFirstByUserOrderByIdDesc(User user);
}
