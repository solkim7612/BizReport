package com.bizreport.core.repository.business;

import com.bizreport.core.entity.history.RefreshHistory;
import com.bizreport.core.entity.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefreshHistoryRepository extends JpaRepository<RefreshHistory, Long> {
    List<RefreshHistory> findByUserOrderByCreatedAtDesc(Users user);
}
