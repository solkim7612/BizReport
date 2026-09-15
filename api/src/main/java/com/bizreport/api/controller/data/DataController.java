package com.bizreport.api.controller.data;

import com.bizreport.core.dto.data.*;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.service.data.DataService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{id}")
    public ResponseEntity<DataListResponse> get(
            @PathVariable("id") String id,
            @ModelAttribute DataRequest request) {

        DataListResponse response = service.get(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> create(
            @PathVariable("id") String id,
            @RequestBody DataCreateRequest request) {

        service.create(id, request);
        return ResponseEntity.ok("수기 세무 데이터 1건 추가 완료");
    }

    @PostMapping(value = "/extract/text/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataCreateRequest> extractText(
            @RequestParam("file") MultipartFile file) {

        DataCreateRequest text = service.extractText(file);
        return ResponseEntity.ok(text);
    }

    @PatchMapping("/{id}/{dataId}")
    public ResponseEntity<String> update(
            @PathVariable("dataId") Long dataId,
            @RequestBody DataUpdateRequest request) {

        service.update(dataId, request);
        return ResponseEntity.ok("데이터 금액 수정 완료");
    }

    @DeleteMapping("/{id}/{dataId}")
    public ResponseEntity<String> delete(
            @PathVariable("dataId") Long dataId) {

        service.delete(dataId);
        return ResponseEntity.ok("데이터 삭제 완료");
    }

    @PostMapping("/generate/mock/{id}")
    public ResponseEntity<List<DataResponse>> generate(
            @PathVariable("id") String id,
            @RequestBody DataGenerateRequest request) {

        List<Data> list = service.generate(id, request);
        List<DataResponse> response = list.stream().map(DataResponse::from).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/format")
    public ResponseEntity<Resource> downloadFormat() throws IOException {
        String filePath = "static/format.csv";
        Resource resource = new ClassPathResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"format.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @PostMapping(value = "/upload/card/{id}", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadCard(
            @PathVariable("id") String id,
            @ModelAttribute DataUploadRequest request) {

        service.uploadCard(id, request);
        return ResponseEntity.ok("특정 카드(" + request.getCardNum() + ") 내역 파일 덮어쓰기 대기열 등록 완료");
    }
}