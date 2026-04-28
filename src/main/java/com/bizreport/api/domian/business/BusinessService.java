package com.bizreport.api.domian.business;

import com.bizreport.api.config.api.APIClient;
import com.bizreport.api.dto.business.RegisterRequest;
import com.bizreport.api.dto.business.StatusResponse;
import com.bizreport.api.entity.global.TaxType;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final UserRepository userRepository;
    private final APIClient apiClient;

    @Transactional
    public User registerOrUpdateBusiness(RegisterRequest request) {
        StatusResponse.Data ntsData = apiClient.getBusinessStatus(request.getBno());

        TaxType parsedTaxType = TaxType.GENERAL;
        if (ntsData.getTax_type().contains("간이과세자")) {
            parsedTaxType = TaxType.SIMPLIFIED;
        }

        LocalDate changeDt = null;
        if (ntsData.getTax_type_change_dt() != null && !ntsData.getTax_type_change_dt().isBlank()) {
            changeDt = LocalDate.parse(ntsData.getTax_type_change_dt(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        return userRepository.findById(request.getBno())
                .map(user -> {
                    user.update(parsedTaxType, ntsData.getB_stt(), changeDt);
                    log.info("사업자 상태 업데이트 완료: {}", user.getId());
                    return user;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .id(request.getBno())
                            .nm(request.getNm())
                            .indCd(request.getIndCd())
                            .indNm(request.getIndNm())
                            .taxType(parsedTaxType)
                            .bStt(ntsData.getB_stt())
                            .taxTypeChangeDt(changeDt)
                            .build();
                    log.info("신규 사업자 등록 완료: {}", newUser.getId());
                    return userRepository.save(newUser);
                });
    }
}