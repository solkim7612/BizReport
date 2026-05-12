package com.bizreport.core.repository;

import com.bizreport.core.repository.batch.BatchRepository;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
    }

    @Test
    @DisplayName("READY 상태인 BatchRequest만 생성일자 오름차순으로 조회되어야 한다")
    void findPendingRequests() throws InterruptedException {
        BatchRequest request1 = new BatchRequest("rateJob", "file1.csv", "data1", null);
        repository.save(request1);

        Thread.sleep(10);

        BatchRequest request2 = new BatchRequest("cardJob", "file2.csv", "data2", null);
        repository.save(request2);

        BatchRequest request3 = new BatchRequest("rateJob", "file3.csv", "data3", null);
        request3.startProcessing();
        repository.save(request3);

        List<BatchRequest> pendingRequests = repository.findByStatusOrderByCreatedAtAsc(BatchStatus.READY);

        assertThat(pendingRequests).hasSize(2);

        assertThat(pendingRequests.get(0).getFileName()).isEqualTo("file1.csv");
        assertThat(pendingRequests.get(1).getFileName()).isEqualTo("file2.csv");

        assertThat(pendingRequests).extracting(BatchRequest::getStatus)
                .containsOnly(BatchStatus.READY);
    }
}