package com.bizreport.api.domain.business;

import com.bizreport.api.config.api.APIClient;
import com.bizreport.core.dto.business.RegisterRequest;
import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.user.User;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.batch.BatchRepository;
import com.bizreport.core.repository.business.HistoryRepository;
import com.bizreport.core.repository.business.RateRepository;
import com.bizreport.core.repository.business.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class BizService {
    private final UserRepository userRepo;
    private final HistoryRepository historyRepo;
    private final RateRepository rateRepo;
    private final BatchRepository batchRepo;
    private final APIClient client;

    @Value("${dir.upload.rate}")
    private String dir;

    @Transactional
    public void register(RegisterRequest request) {
        try {
            StatusResponse.Data data = client.status(request.getId());
            String indNm = getIndNm(request.getIndCd());

            User user = userRepo.findById(request.getId())
                    .orElseGet(() -> userRepo.save(data.toUserEntity(request, indNm)));

            historyRepo.save(user.toHistEntity(user));
            log.info("사업자 등록 완료: {}", user.getId());

        } catch (Exception e) {
            log.error("국세청 상태 조회 API 실패: {}", request.getId(), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED, e);
        }
    }

    // TODO: AWS S3 도입예정
    public void uploadRate(MultipartFile file) {
        validate(file, ".csv");

        try {
            clearDir(dir);
            Path path = Paths.get(dir, file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            BatchRequest request = new BatchRequest("rateJob", file.getOriginalFilename(), null);
            batchRepo.save(request);

            log.info("세율 데이터 파일 업로드 완료 및 배치 작업 대기열 등록: {}", file.getOriginalFilename());

        } catch (Exception e) {
            log.error("파일 업로드 및 배치 실행 중 오류 발생", e);
            throw new CustomException(ErrorCode.BATCH_REGISTRATION_FAILED, e);
        }
    }

    @Transactional
    public void update(String id, RegisterRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String indNm = getIndNm(request.getIndCd());

        user.update(request.getNm(), request.getIndCd(), indNm);
        log.info("B_NO {} : 사용자 정보 수정 완료", user.getId());
    }

    // ==========================================
    // helper method
    // ==========================================

    private String getIndNm(String indCd) {
        return rateRepo.findFirstByIdIndCdOrderByIdYearDesc(indCd)
                .map(TaxRate::getIndNm)
                .orElse("null");
    }

    private void validate(MultipartFile file, String extension) {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private void clearDir(String path) throws IOException {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) f.delete();
                }
            }
        }
    }
}