package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.*;
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
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.google.cloud.vision.v1.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private final UserRepository userRepo;
    private final DataRepository dataRepo;
    private final DataJdbcRepository jdbcRepo;
    private final BatchRepository batchRepo;

    private final ImageAnnotatorClient client;

    @Transactional(readOnly = true)
    public List<DataResponse> getData(String id, DataRequest request) {
        LocalDate startDt = request.getStartYearMonth().atDay(1);
        LocalDate endDt = request.getEndYearMonth().atEndOfMonth();

        return dataRepo.findFilteredData(
                        id,
                        startDt,
                        endDt,
                        request.getType(),
                        request.getMethod()
                ).stream()
                .map(DataResponse::from)
                .toList();
    }

    @Transactional
    public void createData(ManualDataRequest request) {
        Users user = userRepo.findById(request.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        YearMonth targetMon = YearMonth.from(request.getTransDt());
        LocalDate vatDeadline = Reports.getDeadline(ReportType.VAT, targetMon);

        if (LocalDate.now().isAfter(vatDeadline)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        Data data = request.toEntity(user);

        dataRepo.save(data);
        log.info("B_NO {} : 수기 세무 데이터 1건 추가 완료", user.getId());
    }

    public ManualDataRequest extractReceipt(MultipartFile file) {
        validate(file, ".jpg", ".jpeg", ".png");

        try {
            ByteString imgBytes = ByteString.readFrom(file.getInputStream());
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
            AnnotateImageResponse res = response.getResponsesList().get(0);

            if (res.hasError()) {
                throw new CustomException(ErrorCode.OCR_EXTRACTION_FAILED);
            }

            String text = res.getFullTextAnnotation().getText();
            log.info("추출된 영수증 텍스트: \n{}", text);

            return parseText(text);

        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생", e);
            throw new CustomException(ErrorCode.OCR_EXTRACTION_FAILED);
        }
    }

    private ManualDataRequest parseText(String text) {
        ManualDataRequest request = new ManualDataRequest();

        String vendorId = "0000000000";
        Pattern venPattern = Pattern.compile("(?:사업자(?:등록)?번호\\s*[:;]?\\s*)?(\\d{3})\\s*[-]?\\s*(\\d{2})\\s*[-]?\\s*([\\d\\*]{5})");
        Matcher venMatcher = venPattern.matcher(text);
        if (venMatcher.find()) {
            String p1 = venMatcher.group(1);
            String p2 = venMatcher.group(2);
            String p3 = venMatcher.group(3);

            if (!p1.equals("880") || text.contains("사업자")) {
                vendorId = (p1 + p2 + p3).replace("*", "0");
            }
        }

        request.setVendorId(vendorId);

        Pattern datePattern = Pattern.compile("(20\\d{2}|\\d{2})[\\s\\.\\-\\/년]+(0?[1-9]|1[0-2])[\\s\\.\\-\\/월]+(0?[1-9]|[12]\\d|3[01])일?");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            String yearStr = dateMatcher.group(1);
            if (yearStr.length() == 2) yearStr = "20" + yearStr;

            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(dateMatcher.group(2));
            int day = Integer.parseInt(dateMatcher.group(3));

            request.setTransDt(LocalDate.of(year, month, day));
        }

        Pattern totalPattern = Pattern.compile("(?:합\\s*계|결\\s*제\\s*금\\s*액|승\\s*인\\s*금\\s*액|받\\s*을\\s*금\\s*액|신\\s*용\\s*액|총\\s*액|카\\s*드\\s*청\\s*구\\s*액)[\\s:원₩\\\\]*([0-9,]+)");
        Matcher totalMatcher = totalPattern.matcher(text);
        BigDecimal total = BigDecimal.ZERO;
        if (totalMatcher.find()) {
            String totalStr = totalMatcher.group(1).replace(",", "");
            total = new BigDecimal(totalStr);

            request.setTotalPrice(total);
        }

        BigDecimal vat = BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            vat = total.divide(new BigDecimal("11"), 0, java.math.RoundingMode.DOWN);
        }
        request.setVatValue(vat);

        return request;
    }

    @Transactional
    public void updateData(Long id, DataUpdateRequest request) {
        Data data = dataRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        if (!data.isMod()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        data.update(request.netValue(), request.vatValue());
        log.info("데이터 금액 수정 완료: dataId={}", id);
    }

    @Transactional
    public void deleteData(Long id) {
        Data data = dataRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        if (!data.isMod()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        dataRepo.delete(data);
    }

    @Transactional
    public void generate(AutoDataRequest request) {
        Users user = userRepo.findById(request.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Data> dataList = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            dataList.add(request.toEntity(user));
        }

        jdbcRepo.insert(dataList);

        log.info("B_NO {} : 가상 세무 데이터 {}건 생성 및 적재 완료 (기간: {} ~ {})",
                user.getId(), request.getCount(), request.getStartMon(), request.getEndMon());
    }

    public void uploadCard(CardUploadRequest request) {
        validate(request.getFile(), ".csv");

        YearMonth startMon = request.getStartYearMonth();
        YearMonth endMon = request.getEndYearMonth();

        LocalDate startDt = startMon.atDay(1);
        LocalDate endDt = endMon.atEndOfMonth();

        LocalDate citDeadline = Reports.getDeadline(ReportType.CIT, startMon, endMon);
        if (LocalDate.now().isAfter(citDeadline)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        LocalDate vatDeadline = Reports.getDeadline(ReportType.VAT, startMon, endMon);
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
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ==========================================
    // helper method
    // ==========================================

    private void validate(MultipartFile file, String... extensions) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        String fileName = file.getOriginalFilename().toLowerCase();
        boolean isValid = false;
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }
}