package com.bizreport.api.domain.auth;

import com.bizreport.api.dto.auth.SignUpRequest;
import com.bizreport.api.dto.auth.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Auth", description = "인증/회원가입 기능")
public class AuthController {
    private final AuthService service;

    @Operation(description = "사업자등록정보 진위확인 및 상태조회 서비스")
    @GetMapping("/b-no/status")
    public ResponseEntity<StatusResponse> status(@RequestParam("id") String id) {
        String setId = id.replaceAll("-", "");

        return ResponseEntity.ok(service.status(setId));
    }

    @Operation(description = "국세청 인증 후 회원가입")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest request) {
        request.setId(request.getId().replaceAll("-", ""));
        service.signup(request);

        return ResponseEntity.status(201).body("회원가입이 완료되었습니다.");
    }
}
