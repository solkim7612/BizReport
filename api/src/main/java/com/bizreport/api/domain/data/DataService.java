package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.DataRequest;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.user.User;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.batch.BatchRepository;
import com.bizreport.core.repository.data.DataJdbcRepository;
import com.bizreport.core.repository.business.UserRepository;
import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private final UserRepository userRepo;
    private final DataJdbcRepository jdbcRepo;
    private final BatchRepository batchRepo;

    @Value("${dir.upload.card}")
    private String cardDir;

    @Transactional
    public void generate(DataRequest request) {
        User user = userRepo.findById(request.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Data> dataList = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            dataList.add(request.toEntity(user));
        }

        jdbcRepo.insert(dataList);

        log.info("B_NO {} : 가상 세무 데이터 {}건 생성 및 적재 완료 (귀속연도: {})",
                user.getId(), request.getCount(), request.getYear());
    }

    // TODO: AWS S3 도입예정
    public void uploadCard(String id, String startDt, String endDt, MultipartFile file) {
        validate(file, ".csv");
        try {
            String bidDir = cardDir + id + "/";
            clearDir(bidDir);

            Path path = Paths.get(bidDir, file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("id", id);
            paramMap.put("startDt", startDt);
            paramMap.put("endDt", endDt);
            String params = new Gson().toJson(paramMap);

            BatchRequest request = new BatchRequest("cardJob", file.getOriginalFilename(), params);
            batchRepo.save(request);

            log.info("B_NO {} 카드사용내역({}~{}) 배치 작업 대기열 등록: {}", id, startDt, endDt, file.getOriginalFilename());

        } catch (Exception e) {
            log.error("B_NO {} 카드 파일 업로드 오류", id, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    // ==========================================
    // helper method
    // ==========================================

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