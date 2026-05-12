package com.bizreport.api.domain.business;

import com.bizreport.core.dto.business.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BizController.class)
class BizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BizService service;

    @Test
    @DisplayName("사업자 등록 API 테스트")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
         request.setId("1234567890");
         request.setNm("테스트상회");

        String jsonRequest = objectMapper.writeValueAsString(request);

        doNothing().when(service).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사업자 정보 수정 API 테스트")
    void update_Success() throws Exception {
        String id = "1234567890";
        RegisterRequest request = new RegisterRequest();

        String jsonRequest = objectMapper.writeValueAsString(request);

        doNothing().when(service).update(eq(id), any(RegisterRequest.class));

        mockMvc.perform(patch("/api/v1/business/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("세율 데이터 업로드 API 테스트")
    void uploadRate_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tax_rate.csv",
                "text/csv",
                "test data".getBytes()
        );

        doNothing().when(service).uploadRate(any());

        mockMvc.perform(multipart("/api/v1/business/upload/rate")
                        .file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("세율 데이터 업로드 및 배치 시작"));
    }
}