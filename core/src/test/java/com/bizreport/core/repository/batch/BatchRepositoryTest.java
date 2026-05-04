package com.bizreport.core.repository.batch;

import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BatchRepositoryTest {

    @Autowired
    private BatchRepository repository;

    @Test
    @DisplayName("BatchRequest가 정상적으로 저장되고 READY 상태로 조회되어야 한다")
    void saveAndFindPendingRequests() {
        String jobName = "rateJob";
        String fileName = "tax_rate_2026.csv";
        BatchRequest request = new BatchRequest(jobName, fileName, null);

        repository.save(request);

        List<BatchRequest> pendingRequests = repository.findByStatusOrderByCreatedAtAsc(BatchStatus.READY);

        assertThat(pendingRequests).isNotEmpty();
        assertThat(pendingRequests.get(0).getJobName()).isEqualTo(jobName);
        assertThat(pendingRequests.get(0).getStatus()).isEqualTo(BatchStatus.READY);
    }
}