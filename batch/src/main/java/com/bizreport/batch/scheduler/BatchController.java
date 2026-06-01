package com.bizreport.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {
    private final BatchScheduler scheduler;

    @PostMapping("/run/card-upload")
    public ResponseEntity<String> runCardUpload(@RequestParam Long requestId) {
        scheduler.runQueue();
        return ResponseEntity.ok("배치 작업 강제 실행 완료");
    }
}