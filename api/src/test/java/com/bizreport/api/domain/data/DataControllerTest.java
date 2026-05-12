package com.bizreport.api.domain.data;

import com.bizreport.core.dto.data.DataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataService service;

    @Test
    @DisplayName("가상 세무 데이터 생성 API 테스트")
    void generate_Success() throws Exception {
        DataRequest request = new DataRequest();
        request.setId("1234567890");
        request.setYear(2025);
        request.setCount(100);

        String jsonRequest = objectMapper.writeValueAsString(request);

        doNothing().when(service).generate(any(DataRequest.class));

        mockMvc.perform(post("/api/v1/data/generate/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("가상 세무 데이터 생성 완료"));
    }

    @Test
    @DisplayName("포맷 파일 다운로드 API 테스트")
    void downloadFormat_Success() throws Exception {
        mockMvc.perform(get("/api/v1/data/download/format"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=format.csv"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @DisplayName("카드 내역 업로드 API 테스트")
    void uploadCard_Success() throws Exception {
        String id = "1234567890";
        String startDt = "20250101";
        String endDt = "20251231";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "card_history.csv",
                "text/csv",
                "test data".getBytes()
        );

        doNothing().when(service).uploadCard(eq(id), eq(startDt), eq(endDt), any());

        // when & then
        mockMvc.perform(multipart("/api/v1/data/upload/card")
                        .file(file)
                        .param("id", id)
                        .param("startDt", startDt)
                        .param("endDt", endDt))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("카드 내역 파일 덮어쓰기 시작"));
    }
}