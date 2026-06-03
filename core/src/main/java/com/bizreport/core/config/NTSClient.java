package com.bizreport.core.config;

import com.bizreport.core.dto.business.StatusRequest;
import com.bizreport.core.dto.business.StatusResponse;
import com.bizreport.core.entity.exception.CustomException;
import com.bizreport.core.entity.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
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
public class NTSClient {
    private final RestTemplate restTemplate;
    private final String url;
    private final String key;

    public NTSClient(RestTemplate restTemplate, org.springframework.core.env.Environment env) {
        this.restTemplate = restTemplate;

        this.url = env.getProperty("api.nts.url");
        this.key = env.getProperty("NTS_STATUS_KEY");
        log.info("[NTS API] URL: {}, Key: {}", url, key);

        if (key == null) {
            throw new IllegalStateException("NTS_STATUS_KEY가 설정되지 않았습니다!");
        }
    }

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
                log.error("[NTS API] 국세청 API 비정상 응답");
                throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
            }

            if (response.getData() == null) return Collections.emptyList();

            return response.getData();

        } catch (CustomException e) {
            throw e;

        } catch (Exception e) {
            log.error("[NTS API] 국세청 상태조회 API 통신 실패: {}건", userList.size(), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
        }
    }
}