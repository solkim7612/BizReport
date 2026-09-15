package com.bizreport.core.dto.batch;

import com.bizreport.core.entity.batch.BatchRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BatchStatusResponse {
    private Long id;
    private String jobName;
    private String fileName;
    private String status;
    private String jobParameters;
    private LocalDateTime createdAt;

    public static BatchStatusResponse from(BatchRequest request) {
        return BatchStatusResponse.builder()
                .id(request.getId())
                .jobName(request.getJobName())
                .fileName(request.getFileName())
                .status(request.getStatus().name())
                .jobParameters(request.getJobParameters())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
