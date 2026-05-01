package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.DataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService service;

    @PostMapping("/generate/mock")
    public ResponseEntity<String> generate(@RequestBody DataRequest request) {
        service.generate(request);
        return ResponseEntity.ok("가상 세무 데이터 생성 완료");
    }

    @GetMapping("/download/format")
    public ResponseEntity<Resource> downloadFormat() {
        Resource resource = new ClassPathResource("static/format.csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=format.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @PostMapping(value = "/upload/card", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadCard(
            @RequestParam("id") String id, @RequestParam("startDt") String startDt,
            @RequestParam("endDt") String endDt, @RequestParam("file") MultipartFile file) {
        service.uploadCard(id, startDt, endDt, file);
        return ResponseEntity.ok("카드 내역 파일 덮어쓰기 시작");
    }
}