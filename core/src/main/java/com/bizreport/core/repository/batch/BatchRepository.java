package com.bizreport.core.repository.batch;

import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<BatchRequest, Long> {
    List<BatchRequest> findByStatusOrderByCreatedAtAsc(BatchStatus status);
}
