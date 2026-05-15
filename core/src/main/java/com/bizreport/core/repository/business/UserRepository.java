package com.bizreport.core.repository.business;

import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {
    Page<Users> findBySttNot(Status stt, Pageable pageable);

    @Query("SELECT u FROM Users u WHERE u.stt != :closedStatus AND u.endDt IS NOT NULL AND u.endDt <= :today")
    Page<Users> findUsersToClose(@Param("today") LocalDate today, @Param("closedStatus") Status closedStatus, Pageable pageable);
}