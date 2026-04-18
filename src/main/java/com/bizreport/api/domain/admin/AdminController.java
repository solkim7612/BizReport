package com.bizreport.api.domain.admin;

import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "0. Admin", description = "관리자 기능")
public class AdminController {
    private final ImportFileService importService;

    @Operation(description = "경비율 CSV 업로드 및 배치 실행")
    @PostMapping(value = "/tax-rate/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(
            @Parameter(description = "업로드할 CSV 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.MISSING_REQUEST_PARAMETER);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        try {
            log.info("{} UPLOADING_OK", originalFilename);
            importService.importFile(file);
            return ResponseEntity.ok("파일 업로드 성공 및 배치 작업이 실행되었습니다.");

        } catch (Exception e) {
            log.error("UPLOADING_ERROR: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
