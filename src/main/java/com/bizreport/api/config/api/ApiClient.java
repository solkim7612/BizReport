package com.bizreport.api.config.api;

import com.bizreport.api.dto.auth.StatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiClient {
    private final WebClient client;

    @Value("${open-api.nts.service-key}")
    private String key;

    public StatusResponse status(String id) {
        Map<String, List<String>> body = new HashMap<>();
        body.put("b_no", List.of(id));

        log.info("NTS_API_CALL: B_NO {}", id);

        return client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/status")
                        .queryParam("serviceKey", key)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(StatusResponse.class)
                .block();
    }
}

