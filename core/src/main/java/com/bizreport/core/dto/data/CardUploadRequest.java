package com.bizreport.core.dto.data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CardUploadRequest {
    private String id;
    private String cardNum;
    private String startMon;
    private String endMon;
    private MultipartFile file;
}
