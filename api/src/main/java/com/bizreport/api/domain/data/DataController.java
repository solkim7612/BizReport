package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.CardUploadRequest;
import com.bizreport.core.dto.data.DataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService service;

    @Value("${dir.download.card:classpath:static/}")
    private String path;

    @PostMapping("/generate/mock")
    public ResponseEntity<String> generate(@RequestBody DataRequest request) {
        service.generate(request);
        return ResponseEntity.ok("가상 세무 데이터 생성 완료");
    }

    @GetMapping("/download/format")
    public ResponseEntity<Resource> downloadFormat() throws IOException {
        Resource resource = new ClassPathResource(path.replace("classpath:", "") + "format.csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"format.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @PostMapping(value = "/upload/card", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadCard(@ModelAttribute CardUploadRequest request) {
        service.uploadCard(request);
        return ResponseEntity.ok("특정 카드(" + request.getCardNum() + ") 내역 파일 덮어쓰기 대기열 등록 완료");
    }
}