package com.bizreport.api.domian.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {
    private final BatchService service;

    @GetMapping("/download/format")
    public ResponseEntity<Resource> downloadFormat() {
        Resource resource = new ClassPathResource("static/format.csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=format.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @PostMapping(value = "/upload/rate", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadRate(@RequestParam("file") MultipartFile file) {
        service.uploadRate(file);

        return ResponseEntity.ok("세율 데이터 업로드 및 배치가 성공적으로 시작되었습니다.");
    }

    @PostMapping(value = "/upload/card", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadCard(
            @RequestParam("id") String id,
            @RequestParam("file") MultipartFile file) {
        service.uploadCard(id, file);

        return ResponseEntity.ok("B_NO "+id + ": 신용카드 내역 파일 업로드가 완료되었습니다.");
    }
}
