package com.bizreport.core.dto.data;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DataListResponse {
    private List<DataResponse> dataList;
    private DataSummary summary;
}
