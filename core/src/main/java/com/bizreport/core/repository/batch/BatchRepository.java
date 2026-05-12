package com.bizreport.core.repository.batch;

import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<BatchRequest, Long> {
    List<BatchRequest> findByStatusOrderByCreatedAtAsc(BatchStatus status);

    @Modifying
    @Query("UPDATE BatchRequest b SET b.status = 'READY' " +
            "WHERE b.status = 'PROCESSING' AND b.updatedAt < :thresholdTime")
    int recoverZombieRequests(@Param("thresholdTime") LocalDateTime thresholdTime);
}
