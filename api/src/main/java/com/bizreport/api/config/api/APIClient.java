package com.bizreport.api.config.api;

import com.bizreport.core.dto.business.StatusRequest;
import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class APIClient {
    private final RestTemplate restTemplate;

    @Value("${api.nts.url}")
    private String url;

    @Value("${api.nts.key}")
    private String key;

    public StatusResponse.Data status(String b_no) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        StatusRequest request = new StatusRequest(Collections.singletonList(b_no));
        HttpEntity<StatusRequest> entity = new HttpEntity<>(request, headers);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("serviceKey", key)
                    .build(true)
                    .toUri();

            StatusResponse response = restTemplate.postForObject(uri, entity, StatusResponse.class);

            if (response == null || !"OK".equals(response.getStatus_code())) {
                log.error("국세청 API 비정상 응답: {}", response);
                throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
            }

            if (response.getMatch_cnt() == null || response.getMatch_cnt() < 1 || response.getData() == null || response.getData().isEmpty()) {
                log.warn("국세청 API 매칭 결과 없음: {}", b_no);
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            StatusResponse.Data data = response.getData().get(0);

            if ("국세청에 등록되지 않은 사업자등록번호입니다.".equals(data.getTax_type()) || data.getB_stt_cd() == null || data.getB_stt_cd().isBlank()) {
                log.warn("미등록 가짜 사업자 조회 시도됨: {}", b_no);
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            return data;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("국세청 상태조회 API 통신 실패. b_no: {}", b_no, e);
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED, e);
        }
    }
}