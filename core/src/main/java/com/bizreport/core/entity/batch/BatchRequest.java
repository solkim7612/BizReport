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
    @Column(name = "request_id")
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGTEXT", nullable = false)
    private String fileData;

    @Column(name = "job_parameters", columnDefinition = "TEXT")
    private String jobParameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    public BatchRequest(String jobName, String fileName, String fileData, String jobParameters) {
        this.jobName = jobName;
        this.fileName = fileName;
        this.fileData = fileData;
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