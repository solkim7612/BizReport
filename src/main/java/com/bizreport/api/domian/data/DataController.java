package com.bizreport.api.domian.data;

import com.bizreport.api.dto.data.DataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @PostMapping("/generate")
    public ResponseEntity<String> generate(@RequestBody DataRequest request) {
        dataService.generate(request);
        return ResponseEntity.ok(
                request.getId() + " 사업자의 " + request.getTargetYear() + "년도 가상 세무 데이터 "
                        + request.getCount() + "건이 성공적으로 생성되었습니다."
        );
    }
}