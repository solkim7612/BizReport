package com.bizreport.api.domian.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {
    private final JobLauncher launcher;
    private final Job rateJob;
    private final Job cardJob;

    @Value("${batch.upload.rate-dir}")
    private String rateDir;

    @Value("${batch.upload.card-dir}")
    private String cardDir;

    public void uploadRate(MultipartFile file) {
        validate(file, ".csv");

        try {
            clearDir(rateDir);

            Path path = Paths.get(rateDir, file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            log.info("세율 데이터 파일 저장 완료: {}", path.toAbsolutePath());

            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("fileName", file.getOriginalFilename())
                    .toJobParameters();

            launcher.run(rateJob, params);
        } catch (Exception e) {
            log.error("파일 업로드 및 배치 실행 중 오류 발생", e);
            throw new RuntimeException("배치 실행에 실패했습니다.", e);
        }
    }

    public void uploadCard(String id, MultipartFile file) {
        validate(file, ".csv");

        try {
            String bidDir = cardDir + id + "/";
            clearDir(bidDir);

            Path path = Paths.get(bidDir, file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            log.info("B_NO {} 카드 내역 파일 저장 완료: {}", id, path.toAbsolutePath());

            JobParameters params = new JobParametersBuilder()
                    .addString("id", id)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            launcher.run(cardJob, params);
        } catch (Exception e) {
            log.error("B_NO {} 카드 파일 업로드 오류", id, e);
            throw new RuntimeException("카드 내역 업로드에 실패했습니다.", e);
        }
    }

    private void validate(MultipartFile file, String extension){
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(extension)) {
            throw new IllegalArgumentException("유효한 " + extension + " 파일이 아닙니다.");
        }
    }

    private void clearDir(String path) throws IOException{
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) f.delete();
                }
            }
        }
    }
}
