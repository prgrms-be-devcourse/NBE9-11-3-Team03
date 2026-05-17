package com.example.domain.review.service;

import com.example.global.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    // 허용할 확장자 목록
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    // 허용할 MIME 타입 목록
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // 파일 최대 크기: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        validateFile(file);

        try {
            Path copyLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(copyLocation);

            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
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
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("파일 삭제 실패: " + fileName);
        }
    }

    //우효성 검사
    private void validateFile(MultipartFile file) {
        validateNotEmpty(file);
        validateFileSize(file);
        validateFileName(file.getOriginalFilename());
        validateExtension(file.getOriginalFilename());
        validateMimeType(file.getContentType());
        validateMagicBytes(file);
    }

    // 1. 빈 파일 여부 확인
    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("파일이 비어 있습니다.");
        }
    }

    // 2. 파일 크기 확인 (5MB 이하)
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("파일 크기는 5MB 이하만 업로드 가능합니다.");
        }
    }

    // 3. 파일명 유효성 확인 (null, 빈 값, 경로 조작 문자 차단)
    private void validateFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("파일명이 올바르지 않습니다.");
        }

        // 경로 조작 공격 차단: ../ 같은 경로 구분자 포함 여부 확인
        if (originalFilename.contains("..") ||
                originalFilename.contains("/") ||
                originalFilename.contains("\\")) {
            throw new BadRequestException("파일명에 허용되지 않는 문자가 포함되어 있습니다.");
        }
    }

    // 4. 확장자 유효성 확인
    private void validateExtension(String originalFilename) {
        String extension = extractExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "허용되지 않는 파일 형식입니다. 허용 형식: jpg, jpeg, png, gif, webp"
            );
        }
    }

    // 5. MIME 타입 유효성 확인 (확장자 위조 방어)
    private void validateMimeType(String contentType) {
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "허용되지 않는 파일 형식입니다. 이미지 파일만 업로드 가능합니다."
            );
        }
    }
    // 6. 실제 파일 내용(매직 바이트) 검증
    private void validateMagicBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 4) {
                throw new BadRequestException("유효하지 않은 파일입니다.");
            }
            // JPEG: FF D8 FF
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return;
            // PNG: 89 50 4E 47
            if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return;
            // GIF: 47 49 46 38
            if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) return;
            // WebP: RIFF + WEBP (0~3: RIFF, 8~11: WEBP)
            if (bytes.length >= 12 &&
                    bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                    bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return;

            throw new BadRequestException("실제 이미지 파일이 아닙니다.");

        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패", e);
        }
    }
    // 확장자 추출 (점 포함, 예: ".png")
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("파일 확장자를 확인할 수 없습니다.");
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
