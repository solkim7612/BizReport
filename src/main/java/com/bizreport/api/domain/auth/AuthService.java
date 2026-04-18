package com.bizreport.api.domain.auth;

import com.bizreport.api.config.api.ApiClient;
import com.bizreport.api.dto.auth.SignUpRequest;
import com.bizreport.api.dto.auth.StatusResponse;
import com.bizreport.api.dto.history.HistoryRequest;
import com.bizreport.api.entity.history.BizHistory;
import com.bizreport.api.entity.user.BizStatus;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import com.bizreport.api.repository.HistoryRepository;
import com.bizreport.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final HistoryRepository histRepo;
    private final PasswordEncoder passwordEncoder;
    private final ApiClient client;

    public StatusResponse status(String id){
        return client.status(id);
    }

    @Transactional
    public void signup(SignUpRequest request) {
        StatusResponse response = client.status(request.getId());

        if (response.getMatch_cnt() == 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        StatusResponse.StatusData data = response.getData().get(0);

        if (BizStatus.parse(data.getB_stt_cd()) == BizStatus.CLOSED) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String pw = passwordEncoder.encode(request.getPw());
        User user = request.toEntity(data, pw);
        userRepo.save(user);

        BizHistory history = BizHistory.create(user);
        histRepo.save(history);
    }


}
