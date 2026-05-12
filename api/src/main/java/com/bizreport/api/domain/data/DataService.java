package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.DataRequest;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.batch.BatchRepository;
import com.bizreport.core.repository.data.DataJdbcRepository;
import com.bizreport.core.repository.business.UserRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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

    @Transactional
    public void generate(DataRequest request) {
        Users user = userRepo.findById(request.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Data> dataList = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            dataList.add(request.toEntity(user));
        }

        jdbcRepo.insert(dataList);

        log.info("B_NO {} : 가상 세무 데이터 {}건 생성 및 적재 완료 (귀속연도: {})",
                user.getId(), request.getCount(), request.getYear());
    }

    public void uploadCard(String id, String cardNum, String startDt, String endDt, MultipartFile file) {
        validate(file, ".csv");
        try {
            String fileName = file.getOriginalFilename();

            String fileData = new String(file.getBytes(), StandardCharsets.UTF_8);

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("id", id);
            paramMap.put("cardNum", cardNum);
            paramMap.put("startDt", startDt);
            paramMap.put("endDt", endDt);
            String params = new Gson().toJson(paramMap);

            BatchRequest request = new BatchRequest("cardJob", fileName, fileData, params);
            batchRepo.save(request);

            log.info("B_NO {} 카드({}) 사용내역({}~{}) In-Memory 배치 대기열 등록", id, cardNum, startDt, endDt);

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
}