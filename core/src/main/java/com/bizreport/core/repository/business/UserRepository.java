package com.bizreport.core.repository.business;

import com.bizreport.core.entity.user.Status;
import com.bizreport.core.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT u FROM User u WHERE u.endDt IS NOT NULL AND u.endDt < :today AND u.stt != :closedStatus")
    List<User> findUsersToClose(@Param("today") LocalDate today, @Param("closedStatus") Status closedStatus);

    List<User> findAllByIdIn(List<String> ids);
}