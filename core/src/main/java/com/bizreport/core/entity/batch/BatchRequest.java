package com.bizreport.core.entity.batch;

import com.bizreport.core.entity.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "batch_requests")
public class BatchRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "file_name")
    private String fileName;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGTEXT")
    private String fileData;

    @Column(name = "job_parameters", columnDefinition = "TEXT")
    private String jobParameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    @Builder
    public BatchRequest(BatchStatus status, String jobParameters, String fileData, String fileName, String jobName, Long id) {
        this.status = status;
        this.jobParameters = jobParameters;
        this.fileData = fileData;
        this.fileName = fileName;
        this.jobName = jobName;
        this.id = id;
    }

    public BatchRequest(String jobName, String fileName, String fileData, String jobParameters) {
        this.jobName = jobName;
        this.fileName = fileName;
        this.fileData = fileData;
        this.jobParameters = jobParameters;
        this.status = BatchStatus.READY;
    }

    public void update(String jobParameters) {
        this.jobParameters = jobParameters;
    }
    public void processing() {
        this.status = BatchStatus.PROCESSING;
    }

    public void complete() {
        this.status = BatchStatus.COMPLETED;
    }

    public void fail() {
        this.status = BatchStatus.FAILED;
    }
}