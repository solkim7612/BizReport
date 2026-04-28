package com.bizreport.api.domian.data;

import com.bizreport.api.dto.data.DataRequest;
import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import com.bizreport.api.repository.DataRepository;
import com.bizreport.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private final DataRepository dataRepo;
    private final UserRepository userRepo;

    @Transactional
    public void generate(DataRequest request) {
        User user = userRepo.findById(request.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Data> DataList = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            DataList.add(request.toEntity(user));
        }

        dataRepo.saveAll(DataList);

        log.info("B_NO {} : 가상 세무 데이터 {}건 생성 및 적재 완료 (귀속연도: {})",
                user.getId(), request.getCount(), request.getTargetYear());
    }
}