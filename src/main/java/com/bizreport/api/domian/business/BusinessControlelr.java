package com.bizreport.api.domian.business;

import com.bizreport.api.dto.business.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping("/register")
    public ResponseEntity<String> registerBusiness(@RequestBody RegisterRequest request) {
        businessService.registerOrUpdateBusiness(request);
        return ResponseEntity.ok(request.getBno() + " 사업자 등록 및 국세청 상태 조회가 완료되었습니다.");
    }
}