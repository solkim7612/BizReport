package com.bizreport.batch.scheduler;

import com.bizreport.core.dto.batch.BatchStatusResponse;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.batch.BatchRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {
    private final JobLauncher jobLauncher;
    private final BatchRepository batchRepo;

    private final Job cardUploadJob;
    private final Job statusUpdateJob;
    private final Job statusClosedJob;
    private final Job reportCreateJob;
    private final Job rateDeleteJob;
    private final Job dataClosedJob;

    @Transactional
    public void register(String jobName, String jobParameters) {
        BatchRequest request = BatchRequest.builder()
                .jobName(jobName)
                .jobParameters(jobParameters)
                .status(BatchStatus.READY)
                .build();

        batchRepo.saveAndFlush(request);
        log.info("[BATCH] {} 작업 등록", jobName);
    }

    public void execute() {
        List<BatchRequest> readyRequests = batchRepo.findByStatusOrderByCreatedAtAsc(BatchStatus.READY);

        if (readyRequests.isEmpty()) return;

        log.info("[BATCH] 대기 중인 작업 처리 시작: {}건", readyRequests.size());

        for (BatchRequest request : readyRequests) {
            updateStatusToProcessing(request.getId());

            try {
                Job targetJob = resolve(request.getJobName());

                JobParametersBuilder builder = new JobParametersBuilder()
                        .addLong("time", System.nanoTime())
                        .addLong("requestId", request.getId());

                if (request.getJobParameters() != null && !request.getJobParameters().isBlank()) {
                    Map<String, String> paramMap = new Gson().fromJson(
                            request.getJobParameters(),
                            new TypeToken<Map<String, String>>() {}.getType()
                    );
                    paramMap.forEach(builder::addString);
                }

                JobExecution execution = jobLauncher.run(targetJob, builder.toJobParameters());

                update(request.getId(), execution.getStatus().isUnsuccessful());

            } catch (Exception e) {
                log.error("[ERROR] BATCH ID: {} [{}]", request.getId(), e.getMessage());
                update(request.getId(), true);
            }
        }
    }

    @Transactional
    public void updateStatusToProcessing(Long requestId) {
        batchRepo.findById(requestId).ifPresent(req -> {
            req.processing();
            batchRepo.saveAndFlush(req);
        });
    }

    private Job resolve(String jobName) {
        return switch (jobName) {
            case "cardUploadJob" -> cardUploadJob;
            case "statusUpdateJob" -> statusUpdateJob;
            case "statusClosedJob" -> statusClosedJob;
            case "reportCreateJob" -> reportCreateJob;
            case "rateDeleteJob" -> rateDeleteJob;
            case "dataClosedJob" -> dataClosedJob;
            default -> throw new IllegalArgumentException("Unknown job: " + jobName);
        };
    }

    private void update(Long requestId, boolean isFail) {
        batchRepo.findById(requestId).ifPresent(req -> {
            if (isFail) {
                req.fail();
                log.error("[ERROR] BATCH ID: {}", requestId);
            } else {
                req.complete();
                log.info("[BATCH] 작업 성공");
            }
            batchRepo.save(req);
        });
    }

    @Transactional
    public int reapQueue() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        int recoveredCount = batchRepo.recoverZombieRequests(threshold);

        if (recoveredCount > 0) {
            log.warn("[BATCH] 서버 다운으로 멈춰있던 좀비 큐를 READY 상태로 롤백 처리: {}건", recoveredCount);
        }
        return recoveredCount;
    }

    @CacheEvict(value = {"taxRate", "indNm"}, allEntries = true)
    public void clearCache() {
        log.info("[BATCH] 새로운 세율이 적용되어 메모리의 세율 및 업종명 캐시를 모두 초기화");
    }

    @Transactional(readOnly = true)
    public List<BatchStatusResponse> getBatchStatus(String id) {
        String searchParam = "\"id\":\"" + id + "\"";

        return batchRepo.findCardUploadJobsByUserId(searchParam).stream()
                .map(BatchStatusResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BatchStatusResponse> getAllBatchStatus() {
        return batchRepo.findAll().stream()
                .map(BatchStatusResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getBatchFileData(Long batchId) {
        BatchRequest req = batchRepo.findById(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        return req.getFileData();
    }
}