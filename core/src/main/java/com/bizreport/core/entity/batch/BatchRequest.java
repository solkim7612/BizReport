package com.bizreport.core.entity.batch;

import com.bizreport.core.entity.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "batch_requests")
public class BatchRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String jobParameters;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    public BatchRequest(String jobName, String fileName, String jobParameters) {
        this.jobName = jobName;
        this.fileName = fileName;
        this.jobParameters = jobParameters;
        this.status = BatchStatus.READY;
    }

    public void startProcessing() {
        this.status = BatchStatus.PROCESSING;
    }

    public void complete() {
        this.status = BatchStatus.COMPLETED;
    }

    public void fail() {
        this.status = BatchStatus.FAILED;
    }
}