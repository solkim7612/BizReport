package com.bizreport.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchScheduler scheduler;
    private final BatchService service;

    @PostMapping("/queue/run")
    public ResponseEntity<String> runQueue(){

        scheduler.runQueue();
        return ResponseEntity.ok("대기 중인 배치 큐 즉시 처리 완료");
    }

    @PostMapping("/rate/delete")
    public ResponseEntity<String> runDeleteRate() {

        service.runDeleteRate();
        return ResponseEntity.ok("세율 데이터 정리 배치 시작");
    }

    @PostMapping("/status/closed")
    public ResponseEntity<String> runClosedStatus() {

        service.runClosedStatus();
        return ResponseEntity.ok("상태 마감 배치 시작");
    }

    @PostMapping("/status/update")
    public ResponseEntity<String> runUpdateStatus() {

        service.runUpdateStatus();
        return ResponseEntity.ok("상태 업데이트 배치 시작");
    }

    @PostMapping("/data/closed")
    public ResponseEntity<String> runDataClosed(){

        service.runClosedData();
        return ResponseEntity.ok("데이터 마감 배치 시작");
    }

    @PostMapping("/report/monthly")
    public ResponseEntity<String> runReportMonthly() {

        service.runReportMonthly();
        return ResponseEntity.ok("월간 리포트 생성 배치 시작");
    }

    @PostMapping("/report/accumulated")
    public ResponseEntity<String> runReportAccumulated() {

        service.runReportAccumulated();
        return ResponseEntity.ok("누적 리포트 생성 배치 시작");
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {

        service.clearRateCache();
        return ResponseEntity.ok("세율 및 업종명 캐시 초기화 완료");
    }
}