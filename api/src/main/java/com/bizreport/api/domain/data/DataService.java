package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.CardUploadRequest;
import com.bizreport.core.dto.data.DataRequest;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.data.Data;
import com.bizreport.core.entity.data.DataMethod;
import com.bizreport.core.entity.report.ReportType;
import com.bizreport.core.entity.report.Reports;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.batch.BatchRepository;
import com.bizreport.core.repository.data.DataJdbcRepository;
import com.bizreport.core.repository.business.UserRepository;
import com.bizreport.core.repository.data.DataRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private final UserRepository userRepo;
    private final DataRepository dataRepo;
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

    public void uploadCard(CardUploadRequest request) {
        validate(request.getFile(), ".csv");

        YearMonth endMon = YearMonth.parse(request.getEndMon());
        LocalDate startDt = YearMonth.parse(request.getStartMon()).atDay(1);
        LocalDate endDt = endMon.atEndOfMonth();

        LocalDate citDeadline = Reports.getDeadline(ReportType.CIT, endMon);
        if (LocalDate.now().isAfter(citDeadline)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        LocalDate vatDeadline = Reports.getDeadline(ReportType.VAT, endMon);
        boolean isPassed = LocalDate.now().isAfter(vatDeadline);
        boolean ignoreVat = false;

        if (isPassed) {
            boolean exists = dataRepo.existsByUserIdAndMethodAndCardNumAndTransDtBetween(
                    request.getId(), DataMethod.CARD, request.getCardNum(), startDt, endDt);

            if (exists) {
                throw new CustomException(ErrorCode.REPORT_ALREADY_CLOSED);
            } else {
                ignoreVat = true;
            }
        }

        try {
            String fileName = request.getFile().getOriginalFilename();
            String fileData = new String(request.getFile().getBytes(), StandardCharsets.UTF_8);

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("id", request.getId());
            paramMap.put("cardNum", request.getCardNum());
            paramMap.put("startDt", startDt.toString());
            paramMap.put("endDt", endDt.toString());
            paramMap.put("ignoreVat", String.valueOf(ignoreVat));

            String params = new Gson().toJson(paramMap);

            BatchRequest batchReq = new BatchRequest("cardUploadJob", fileName, fileData, params);
            batchRepo.save(batchReq);

            log.info("B_NO {} 카드({}) In-Memory 배치 대기열 등록 (ignoreVat: {})", request.getId(), request.getCardNum(), ignoreVat);

        } catch (Exception e) {
            log.error("B_NO {} 카드 파일 업로드 오류", request.getId(), e);
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