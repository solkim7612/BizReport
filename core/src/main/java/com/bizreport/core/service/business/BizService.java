package com.bizreport.core.service.business;

import com.bizreport.core.config.NTSClient;
import com.bizreport.core.dto.business.*;
import com.bizreport.core.entity.history.RefreshHistory;
import com.bizreport.core.entity.rate.TaxRate;
import com.bizreport.core.entity.rate.VatRate;
import com.bizreport.core.entity.user.Users;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import com.bizreport.core.repository.business.BizHistoryRepository;
import com.bizreport.core.repository.business.RateRepository;
import com.bizreport.core.repository.business.RefreshHistoryRepository;
import com.bizreport.core.repository.business.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BizService {
    private final UserRepository userRepo;
    private final BizHistoryRepository historyRepo;
    private final RefreshHistoryRepository refreshHistoryRepo;
    private final RateRepository rateRepo;
    private final NTSClient client;

    @Value("${chunk.size.business:500}")
    private int chunk;

    @Transactional
    public void register(RegisterRequest request) {
        try {
            if (userRepo.existsById(request.getId())) {
                log.warn("[BIZ] 이미 등록된 사업자 등록 시도: {}", request.getId());
                throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
            }

            StatusResponse.Data data = client.status(request.getId());

            if (data.getB_stt_cd() == null || data.getB_stt_cd().isBlank()) {
                log.warn("[BIZ] 유효하지 않은 사업자 번호 등록 시도: {}", request.getId());
                throw new CustomException(ErrorCode.INVALID_BUSINESS_NUMBER);
            }

            if ("03".equals(data.getB_stt_cd())) {
                log.warn("[BIZ] 폐업 사업자 등록 시도: {}", request.getId());
                throw new CustomException(ErrorCode.USER_ALREADY_CLOSED);
            }

            String indNm = getIndNm(request.getIndCd());

            Users user = userRepo.save(data.toUserEntity(request, indNm));
            historyRepo.save(user.toHistEntity(user));

            log.info("[BIZ] 사업자 등록 완료: B_NO {}", user.getId());

        } catch (CustomException e) {
            throw e;

        } catch (Exception e) {
            log.error("[BIZ] 국세청 상태 조회 API 실패: B_NO {}", request.getId(), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public void check(String id) {
        boolean exists = userRepo.existsById(id);
        if (!exists) {
            log.error("[BIZ] 등록되지 않은 사용자: {}", id);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse get(String id) {
        Users user = getUser(id);

        return new UserResponse(
                user.getNm(),
                user.getIndCd(),
                user.getIndNm(),
                user.getRefreshCount(),
                user.getTaxType() != null ? user.getTaxType().getDesc() : null
        );
    }

    @Transactional
    public void update(String id, UpdateRequest request) {
        Users user = getUser(id);

        String indNm = getIndNm(request.getIndCd());

        user.update(request.getNm(), request.getIndCd(), indNm);
        log.info("[BIZ] 사용자 정보 수정 완료: B_NO {}", user.getId());
    }

    @Transactional
    public void charge(String id, int count) {
        Users user = getUser(id);

        user.charge(count);
        refreshHistoryRepo.save(new RefreshHistory(user, "CHARGE", count, user.getRefreshCount()));
        log.info("[BIZ] 갱신권 충전 완료: B_NO {}, 충전개수 {}, 변경 후 잔여횟수 {}",
                id, count, user.getRefreshCount());
    }

    @Transactional(readOnly = true)
    public List<BizHistoryResponse> getBizHistory(String id) {
        Users user = getUser(id);

        return historyRepo.findByUserOrderByCreatedAtDesc(user).stream()
                .map(BizHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RefreshHistoryResponse> getRefreshHistory(String id) {
        Users user = getUser(id);

        return refreshHistoryRepo.findByUserOrderByCreatedAtDesc(user).stream()
                .map(RefreshHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IndResponse> searchIndNm(String keyword) {

        return rateRepo.findByIndNmContaining(keyword).stream()
                .map(rate -> new IndResponse(rate.getId().getIndCd(), rate.getIndNm()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IndResponse> searchIndCd(String keyword) {

        return rateRepo.findByIndCdContaining(keyword).stream()
                .map(rate -> new IndResponse(rate.getId().getIndCd(), rate.getIndNm()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void uploadRate(MultipartFile file) {
        validate(file, ".csv");

        try {
            String rawData = new String(file.getBytes(), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("\"([^\"]*)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(rawData);
            StringBuilder sb = new StringBuilder();

            while (matcher.find()) {
                String content = matcher.group(1).replaceAll("\\r?\\n", " ").replace(",", " ");
                matcher.appendReplacement(sb, "\"" + content + "\"");
            }
            matcher.appendTail(sb);
            String parsingData = sb.toString();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new java.io.ByteArrayInputStream(parsingData.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {

                String line;
                int lineCount = 0;
                int savedCount = 0;
                List<TaxRate> list = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (lineCount <= 3) {
                        continue;
                    }

                    String[] fields = line.split(",");
                    if (fields.length < 10) continue;

                    // 필드 순서 매핑:
                    // 0: year, 1: indCd, 2: indNm, 3: category1, 4: category2, 5: category3, 6: msg, 7: expRt, 8: overExpRt, 9: stndExpRt
                    String year = fields[0].trim().replace("\"", "");
                    String indCd = fields[1].trim().replace("\"", "");
                    String indNm = fields[2].trim().replace("\"", "");
                    String category1 = fields[3].trim().replace("\"", "");
                    String category2 = fields[4].trim().replace("\"", "");
                    String category3 = fields[5].trim().replace("\"", "");
                    BigDecimal expRt = new BigDecimal(fields[7].trim().replace("\"", "").isBlank() ? "0" : fields[7].trim().replace("\"", ""));
                    BigDecimal vatRt = categorize(category1, category2, category3);

                    TaxRate rate = new TaxRate(indCd, year, indNm, vatRt, expRt);
                    list.add(rate);

                    if (list.size() >= chunk) {
                        rateRepo.saveAll(list);
                        savedCount += list.size();
                        list.clear();
                    }
                }

                if (!list.isEmpty()) {
                    rateRepo.saveAll(list);
                    savedCount += list.size();
                }

                log.info("[BIZ] 세율 파일 업로드 완료: 총 {}건", savedCount);
            }

        } catch (Exception e) {
            log.error("[BIZ] 세율 파일 업로드 처리 중 오류 발생", e);
            throw new CustomException(ErrorCode.BATCH_REGISTRATION_FAILED);
        }
    }


    // ==========================================
    // helper method
    // ==========================================

    private Users getUser(String id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String getIndNm(String indCd) {
        return rateRepo.findIndustryNameByCode(indCd).orElse("미분류 업종");
    }

    private void validate(MultipartFile file, String extension) {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private BigDecimal categorize(String category1, String category2, String category3) {
        String c1 = (category1 == null) ? "" : category1;
        String c2 = (category2 == null) ? "" : category2;
        String c3 = (category3 == null) ? "" : category3;

        String category = (c1 + c2 + c3).replaceAll("\\s", "");

        if (containsKey(category, "소매업", "재생용", "음식점업")) return VatRate.RT_15.getRate();
        if (containsKey(category, "제조업", "농업", "임업", "어업", "소화물")) return VatRate.RT_20.getRate();
        if (containsKey(category, "숙박업")) return VatRate.RT_25.getRate();
        if (containsKey(category, "금융", "보험", "기술서비스업", "사업시설", "부동산")) return VatRate.RT_40.getRate();
        if (category.contains("기술서비스업") && category.contains("인물")) return VatRate.RT_30.getRate();

        return VatRate.RT_30.getRate();
    }

    private boolean containsKey(String target, String... keywords) {
        for (String key : keywords) {
            if (target.contains(key)) return true;
        }

        return false;
    }
}