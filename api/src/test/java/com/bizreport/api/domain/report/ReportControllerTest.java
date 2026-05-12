package com.bizreport.api.domain.report;

import com.bizreport.core.dto.report.ReportRequest;
import com.bizreport.core.dto.report.ReportResponse;
import com.bizreport.core.entity.report.ReportType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService service;

    @Test
    @DisplayName("누적 리포트 생성 API 테스트")
    void generateAccumulated_Success() throws Exception {
        ReportRequest request = new ReportRequest();
        request.setId("1234567890");
        request.setReportType(ReportType.VAT);
        request.setStartMon("2026-01");
        request.setEndMon("2026-12");
        request.setPrepaidTax(BigDecimal.ZERO);

        String jsonRequest = objectMapper.writeValueAsString(request);

        ReportResponse mockResponse = Mockito.mock(ReportResponse.class);
        when(service.generateAccumulated(any(ReportRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/reports/batch/accumulated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리포트 조회 API 테스트")
    void getReport_Success() throws Exception {
        String id = "1234567890";
        String startMon = "2026-01";

        ReportResponse mockResponse = Mockito.mock(ReportResponse.class);
        when(service.getReport(any(ReportRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/reports/view/{id}", id)
                        .param("startMon", startMon)
                        .param("reportType", "VAT"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}