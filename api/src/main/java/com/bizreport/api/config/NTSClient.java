package com.bizreport.api.config;

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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NTSClient {
    private final RestTemplate restTemplate;

    @Value("${api.nts.url}")
    private String url;

    @Value("${api.nts.key}")
    private String key;

    public StatusResponse.Data status(String id) {
        return status(Collections.singletonList(id)).stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public List<StatusResponse.Data> status(List<String> userList) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        StatusRequest request = new StatusRequest(userList);
        HttpEntity<StatusRequest> entity = new HttpEntity<>(request, headers);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("serviceKey", key)
                    .build(true)
                    .toUri();

            StatusResponse response = restTemplate.postForObject(uri, entity, StatusResponse.class);

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            if (response == null || !"OK".equals(response.getStatus_code())) {
                log.error("국세청 API 비정상 응답");
                throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
            }
            if (response.getData() == null) return Collections.emptyList();

            return response.getData();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("국세청 상태조회 API 통신 실패. b_no count: {}", userList.size(), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
        }
    }
}