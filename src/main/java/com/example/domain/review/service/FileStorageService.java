package com.example.domain.review.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        try {
            Path copyLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(copyLocation);

            // 확장자만 추출 (예: .png)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // UUID로만 파일명 생성 (공백/한글 문제 완벽 차단)
            String fileName = UUID.randomUUID().toString() + extension;

            Path targetLocation = copyLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;

        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
            Files.deleteIfExists(filePath); // 물리적 파일 삭제!
        } catch (IOException e) {
            // 삭제 실패 시 에러를 던지기보단 로그만 남기는 것이 안전할 수 있습니다.
            System.err.println("파일 삭제 실패: " + fileName);
        }
    }
}
