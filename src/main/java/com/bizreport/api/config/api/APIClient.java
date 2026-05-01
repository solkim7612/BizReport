package com.bizreport.api.config.api;

import com.bizreport.core.dto.business.StatusRequest;
import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.exception.CustomException;
import com.bizreport.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

        StatusRequest request = new StatusRequest(Collections.singletonList(b_no));
        HttpEntity<StatusRequest> entity = new HttpEntity<>(request, headers);

        try {
            URI uri = new URI(url + "?serviceKey=" + key);
            StatusResponse response = restTemplate.postForObject(uri, entity, StatusResponse.class);

            if (response == null || !"OK".equals(response.getStatus_code()) || response.getData().isEmpty()) {
                throw new IllegalStateException("국세청 API 응답이 올바르지 않습니다.");
            }

            StatusResponse.Data data = response.getData().get(0);

            // TODO: TaxType Entity 에서 api에서 tax_type_cd 받을 경우 entity enum 으로 파싱 필요
            // TODO:
            if ("국세청에 등록되지 않은 사업자등록번호입니다.".equals(data.getTax_type())) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            return data;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("국세청 상태조회 API 호출 실패", e);
            throw new RuntimeException("국세청 통신 오류: " + e.getMessage());
        }
    }
}
