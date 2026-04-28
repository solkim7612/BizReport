package com.bizreport.api.domian.data;

import com.bizreport.api.dto.data.DataRequest;
import com.bizreport.api.entity.data.Data;
import com.bizreport.api.entity.user.User;
import com.bizreport.api.repository.DataRepository;
import com.bizreport.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataService {
    private final DataRepository dataRepo;
    private final UserRepository userRepo;

    @Transactional
    public void generateRandomData(DataRequest request) {
        User user = userRepo.findById(request.getId()).orElseThrow();
        List<Data> dataList = new ArrayList<>();

        for (int i = 0; i < request.getCount(); i++) {
            dataList.add(request.toEntity(user));
        }
        dataRepo.saveAll(dataList);
    }
}