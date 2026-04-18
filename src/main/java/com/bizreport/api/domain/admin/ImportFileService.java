package com.bizreport.api.domain.admin;

import com.bizreport.api.entity.rate.RateId;
import com.bizreport.api.entity.rate.TaxRate;
import com.bizreport.api.exception.CustomException;
import com.bizreport.api.exception.ErrorCode;
import com.bizreport.api.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportFileService {
    private final JobLauncher launcher;
    private final Job job;
    private final TaxRateRepository repository;

    @Value("${file.upload.temp-dir}")
    private String tempDir;

    public void importFile(MultipartFile file) throws Exception {
        String filePath = saveFile(file);

        JobParameters parameters = new JobParametersBuilder()
                .addString("filePath", filePath)
                .addString("timestamp", LocalDateTime.now().toString())
                .toJobParameters();

        launcher.run(job, parameters);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String name = "tax_rate_" + System.currentTimeMillis() + ".csv";
        Path path = Paths.get(tempDir, name);

        File tempDir = path.getParent().toFile();
        if (!tempDir.exists()) tempDir.mkdirs();

        file.transferTo(path.toFile());
        return path.toString();
    }

    // TODO: 나중에 사용
    @Transactional(readOnly = true)
    public TaxRate getTaxRate(RateId id) {
        return repository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.TAX_RATE_NOT_FOUND));
    }
}
