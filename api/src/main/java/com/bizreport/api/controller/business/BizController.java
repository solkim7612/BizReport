package com.bizreport.api.controller.business;

import com.bizreport.core.dto.business.RegisterRequest;
import com.bizreport.core.service.business.BizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BizController {
    private final BizService service;

    @PostMapping
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {

        service.register(request);
        return ResponseEntity.ok(request.getId() + " 사업자 등록 완료");
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @RequestBody RegisterRequest request) {

        service.update(id, request);
        return ResponseEntity.ok(id + " 사업자 정보 수정 완료");
    }

    @PostMapping(value = "/upload/rate", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadRate(@RequestParam("file") MultipartFile file) {

        service.uploadRate(file);
        return ResponseEntity.ok("세율 데이터 업로드 완료 및 배치 대기열 등록 성공");
    }
}