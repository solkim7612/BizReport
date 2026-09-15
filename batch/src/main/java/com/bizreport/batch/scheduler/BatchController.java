package com.bizreport.batch.scheduler;

import com.bizreport.core.dto.batch.BatchStatusResponse;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService service;

    @PostMapping("/job/statusUpdateJob")
    public ResponseEntity<String> runUpdateStatus() {
        service.register("statusUpdateJob", null);

        return ResponseEntity.ok("상태 업데이트 배치 큐 등록 완료");
    }

    @PostMapping("/job/statusClosedJob")
    public ResponseEntity<String> runClosedStatus() {
        service.register("statusClosedJob", null);

        return ResponseEntity.ok("상태 마감 배치 큐 등록 완료");
    }

    @PostMapping("/job/reportCreateJob")
    public ResponseEntity<String> runCreateReport(@RequestParam(required = false) String endMon) {
        YearMonth targetMon = (endMon != null && !endMon.isBlank())
                ? YearMonth.parse(endMon)
                : YearMonth.now().minusMonths(1);

        String citParams = new Gson().toJson(Map.of("reportType", "CIT", "endMon", targetMon.toString()));
        String vatParams = new Gson().toJson(Map.of("reportType", "VAT", "endMon", targetMon.toString()));

        service.register("reportCreateJob", citParams);
        service.register("reportCreateJob", vatParams);

        return ResponseEntity.ok("월간/누적 리포트 생성 배치 큐 등록 완료");
    }

    @PostMapping("/job/rateDeleteJob")
    public ResponseEntity<String> runDeleteRate() {
        service.register("rateDeleteJob", null);

        return ResponseEntity.ok("세율 데이터 정리 배치 큐 등록 완료");
    }

    @PostMapping("/job/dataClosedJob")
    public ResponseEntity<String> runClosedData(){
        service.register("dataClosedJob", null);

        return ResponseEntity.ok("데이터 마감 배치 큐 등록 완료");
    }

    @PostMapping("/cache")
    public ResponseEntity<String> clearCache() {
        service.clearCache();

        return ResponseEntity.ok("세율 및 업종명 캐시 초기화 완료");
    }

    @PostMapping("/queue/reap")
    public ResponseEntity<String> runReapQueue() {
        int count = service.reapQueue();

        return ResponseEntity.ok("좀비 큐 롤백 처리 완료: " + count + "건");
    }

    @GetMapping("/queue/{id}")
    public ResponseEntity<List<BatchStatusResponse>> getBatchStatus(@PathVariable("id") String id) {

        return ResponseEntity.ok(service.getBatchStatus(id));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<BatchStatusResponse>> getAllBatches() {

        return ResponseEntity.ok(service.getAllBatchStatus());
    }

    @GetMapping("/queue/detail/{batchId}")
    public ResponseEntity<String> getBatchFileData(@PathVariable("batchId") Long batchId) {

        return ResponseEntity.ok(service.getBatchFileData(batchId));
    }
}