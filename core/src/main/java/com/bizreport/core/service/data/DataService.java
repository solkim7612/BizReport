package com.bizreport.core.service.data;

import com.bizreport.core.dto.data.*;
import com.bizreport.core.entity.batch.BatchRequest;
import com.bizreport.core.entity.batch.BatchStatus;
import com.bizreport.core.entity.data.Data;
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
    public DataListResponse get(String id, DataRequest request) {
        LocalDate startDt = request.getStartYearMonth().atDay(1);
        LocalDate endDt = request.getEndYearMonth().atEndOfMonth();

        List<Data> list = dataRepo.findFilteredData(id, startDt, endDt, request.getType(), request.getMethod());
        List<DataResponse> response = list.stream().map(DataResponse::from).toList();

        DataSummary summary = null;
        if (request.isFilter()) {
            summary = DataSummary.builder()
                    .count(list.size())
                    .totalNetValue(list.stream().map(Data::getNetValue).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .totalVatValue(list.stream().map(Data::getVatValue).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .totalPrice(list.stream().map(Data::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .build();
        }

        return DataListResponse.builder().dataList(response).summary(summary).build();
    }

    @Transactional
    public void create(String id, DataCreateRequest request) {
        Users user = getUser(id);

        YearMonth targetMon = YearMonth.from(request.getTransDt());
        LocalDate vatDeadline = Reports.getDeadline(ReportType.VAT, targetMon);
        LocalDate citDeadline = Reports.getDeadline(ReportType.CIT, targetMon);

        if (LocalDate.now().isAfter(citDeadline)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_CLOSED);
        }

        boolean ignoreVat = LocalDate.now().isAfter(vatDeadline);

        Data data = request.toEntity(user, ignoreVat);
        dataRepo.save(data);

        log.info("[DATA] 수기 세무 데이터 1건 추가 완료 (ignoreVat={}): B_NO {}", ignoreVat, user.getId());
    }

    public DataCreateRequest extractText(MultipartFile file) {
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
            log.info("[DATA] 추출된 영수증 텍스트: \n{}", text);

            return parseText(text);

        } catch (Exception e) {
            log.error("[DATA] OCR 처리 중 오류 발생", e);
            throw new CustomException(ErrorCode.OCR_EXTRACTION_FAILED);
        }
    }

    private DataCreateRequest parseText(String text) {
        DataCreateRequest request = new DataCreateRequest();

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

        BigDecimal total = BigDecimal.ZERO;

        Pattern totalPattern = Pattern.compile("(?:합\\s*계|결\\s*제\\s*금\\s*액|승\\s*인\\s*금\\s*액|받\\s*을\\s*금\\s*액|신\\s*용\\s*액|총\\s*액|카\\s*드\\s*청\\s*구\\s*액)[\\s:원₩\\\\]*([0-9,]+)");
        Matcher totalMatcher = totalPattern.matcher(text);
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
    public void update(Long id, DataUpdateRequest request) {
        Data data = getData(id);

        if (!data.isMod()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        data.update(request.netValue(), request.vatValue());
        log.info("[DATA] 데이터 금액 수정 완료: dataId={}", id);
    }

    @Transactional
    public void delete(Long id) {
        Data data = getData(id);

        if (!data.isMod()) {
            log.error("[DATA] 수정할 수 없는 데이터: dataId={}", id);
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        dataRepo.delete(data);
    }

    @Transactional
    public List<Data> generate(String id, DataGenerateRequest request) {
        Users user = getUser(id);

        List<Data> list = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            list.add(request.toEntity(user));
        }

        jdbcRepo.insert(list);
        log.info("[DATA] B_NO {} 의 해당 기간({} ~ {}) 가상 세무 데이터 생성: {}건 ",
                user.getId(), request.getStartMon(), request.getEndMon(), request.getCount());

        return list;
    }

    public void uploadCard(String id, DataUploadRequest request) {
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
        boolean ignoreVat = isPassed;

        try {
            String fileName = request.getFile().getOriginalFilename();
            String fileData = new String(request.getFile().getBytes(), StandardCharsets.UTF_8);

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("id", id.replaceAll("-", ""));
            paramMap.put("cardNum", request.getCleanCardNum());
            paramMap.put("startDt", startDt.toString());
            paramMap.put("endDt", endDt.toString());
            paramMap.put("ignoreVat", String.valueOf(ignoreVat));
            paramMap.put("fileName", fileName);

            String jobParameters = new Gson().toJson(paramMap);

            BatchRequest batchRequest = BatchRequest.builder()
                    .jobName("cardUploadJob")
                    .fileName(fileName)
                    .fileData(fileData)
                    .jobParameters(jobParameters)
                    .status(BatchStatus.READY)
                    .build();

            batchRepo.saveAndFlush(batchRequest);

            log.info("[DATA] B_NO {} 의 카드 파일 업로드 배치 대기열 등록: 카드({}), 부가세포함여부({})",
                    id, request.getCardNum(), ignoreVat);

        } catch (Exception e) {
            log.error("[DATA] B_NO {} 의 카드 파일 업로드 오류", id, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    // ==========================================
    // helper method
    // ==========================================

    private Users getUser(String id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Data getData(Long id) {
        return dataRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }

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