package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService service;

    @Value("${dir.download.card:classpath:static/}")
    private String path;

    @GetMapping("/{id}")
    public ResponseEntity<List<DataResponse>> getData(
            @PathVariable("id") String id,
            @ModelAttribute DataRequest request) {

        List<DataResponse> response = service.getData(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<String> createData(@RequestBody ManualDataRequest request) {
        service.createData(request);
        return ResponseEntity.ok("수기 세무 데이터 1건 추가 완료");
    }

    @PostMapping(value = "/receipt/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ManualDataRequest> extractReceipt(@RequestParam("file") MultipartFile file) {

        ManualDataRequest extractedData = service.extractReceipt(file);

        return ResponseEntity.ok(extractedData);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> updateData(
            @PathVariable("id") Long id,
            @RequestBody DataUpdateRequest request) {

        service.updateData(id, request);
        return ResponseEntity.ok("데이터 금액 수정 완료");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteData(@PathVariable("id") Long id) {
        service.deleteData(id);
        return ResponseEntity.ok("데이터 삭제 완료");
    }

    @PostMapping("/generate/mock")
    public ResponseEntity<String> generate(@RequestBody AutoDataRequest request) {
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