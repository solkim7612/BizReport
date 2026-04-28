package com.bizreport.api.dto.business;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String bno;
    private String nm;
    private String indCd;
    private String indNm;
}
