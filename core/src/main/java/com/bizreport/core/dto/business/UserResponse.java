package com.bizreport.core.dto.business;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
    private String nm;
    private String indCd;
    private String indNm;
    private int refreshCount;
    private String taxType;
}
