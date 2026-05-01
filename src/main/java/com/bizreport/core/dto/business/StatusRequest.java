package com.bizreport.core.dto.business;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StatusRequest {
    private List<String> b_no;

//    {
//        "b_no": [
//        "0000000000"
//     ]
//    }
}
