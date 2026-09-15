package com.bizreport.api.controller.business;

import com.bizreport.core.dto.business.*;
import com.bizreport.core.service.business.BizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BizController {
    private final BizService service;

    @PostMapping
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        service.register(request);

        return ResponseEntity.ok(request.getId() + " 사업자의 등록 완료");
    }

    @GetMapping("/check/{id}")
    public ResponseEntity<Void> check(@PathVariable String id) {
        service.check(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable String id) {
        UserResponse response = service.get(id);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @RequestBody UpdateRequest request) {

        service.update(id, request);

        return ResponseEntity.ok(id + " 사업자의 정보 수정 완료");
    }

    @PatchMapping("/charge/refresh")
    public ResponseEntity<String> charge(
            @RequestParam String id,
            @RequestParam int count) {

        service.charge(id, count);

        return ResponseEntity.ok(id + " 사업자의 갱신권 충전 완료");
    }

    @GetMapping("/history/biz/{id}")
    public ResponseEntity<List<BizHistoryResponse>> getBizHistory(@PathVariable String id) {

        return ResponseEntity.ok(service.getBizHistory(id));
    }

    @GetMapping("/history/refresh/{id}")
    public ResponseEntity<List<RefreshHistoryResponse>> getRefreshHistory(@PathVariable String id) {

        return ResponseEntity.ok(service.getRefreshHistory(id));
    }

    @GetMapping("/search/indNm")
    public ResponseEntity<List<IndResponse>> searchIndNm(@RequestParam String keyword) {
        List<IndResponse> response = service.searchIndNm(keyword);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/indCd")
    public ResponseEntity<List<IndResponse>> searchIndCd(@RequestParam String keyword) {
        List<IndResponse> response = service.searchIndCd(keyword);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/rate", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadRate(@RequestParam("file") MultipartFile file) {
        service.uploadRate(file);

        return ResponseEntity.ok("세율 데이터 업로드 완료");
    }
}