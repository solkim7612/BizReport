package com.bizreport.core.entity;

import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchRequestTest {

    @Test
    @DisplayName("BatchRequest 생성 시 초기 상태는 항상 READY이다")
    void createBatchRequest() {
        BatchRequest request = new BatchRequest("rateJob", "test.csv", "data...", null);

        assertThat(request.getStatus()).isEqualTo(BatchStatus.READY);
    }

    @Test
    @DisplayName("startProcessing() 호출 시 상태가 PROCESSING으로 변경된다")
    void startProcessing() {
        BatchRequest request = new BatchRequest("rateJob", "test.csv", "data...", null);

        request.startProcessing();

        assertThat(request.getStatus()).isEqualTo(BatchStatus.PROCESSING);
    }

    @Test
    @DisplayName("complete()와 fail() 호출 시 각각 COMPLETED와 FAILED로 상태가 변경된다")
    void completeAndFail() {
        BatchRequest request1 = new BatchRequest("job1", "file1.csv", "data", null);
        BatchRequest request2 = new BatchRequest("job2", "file2.csv", "data", null);

        request1.complete();
        request2.fail();

        assertThat(request1.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(request2.getStatus()).isEqualTo(BatchStatus.FAILED);
    }
}