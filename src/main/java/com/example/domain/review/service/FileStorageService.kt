package com.example.domain.review.service

import com.example.global.exception.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    companion object {
        private val log = LoggerFactory.getLogger(FileStorageService::class.java)
        // 허용할 확장자 목록
        private val ALLOWED_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".gif", ".webp")

        // 허용할 MIME 타입 목록
        private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp")

        // 파일 최대 크기: 5MB
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L
    }

    fun storeFile(file: MultipartFile): String {
        validateFile(file)

        return try {
            val copyLocation = Paths.get(uploadDir).toAbsolutePath().normalize()
            Files.createDirectories(copyLocation)

            val originalFilename = file.originalFilename
                ?: throw BadRequestException("파일명이 존재하지 않습니다.")
            val extension = extractExtension(originalFilename)
            val fileName = "${UUID.randomUUID()}$extension"

            Files.copy(file.inputStream, copyLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING)

            fileName
        } catch (e: IOException) {
            throw RuntimeException("파일 저장 실패", e)
        }
    }

    fun deleteFile(fileName: String?) {
        if (fileName.isNullOrEmpty()) return

        try {
            val filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName)
            Files.deleteIfExists(filePath)
        } catch (e: IOException) {
            log.error("파일 삭제 실패: $fileName", e)
        }
    }

    //유효성 검사
    private fun validateFile(file: MultipartFile) {
        validateNotEmpty(file)
        validateFileSize(file)
        val originalFilename = file.originalFilename ?: throw BadRequestException("파일명이 존재하지 않습니다.")
        val contentType = file.contentType ?: throw BadRequestException("파일의 Content-Type을 확인할 수 없습니다.")
        validateFileName(originalFilename)
        validateExtension(originalFilename)
        validateMimeType(contentType)
        validateMagicBytes(file)
    }

    // 1. 빈 파일 여부 확인
    private fun validateNotEmpty(file: MultipartFile) {
        if (file.isEmpty) throw BadRequestException("파일이 비어 있습니다.")
    }

    // 2. 파일 크기 확인 (5MB 이하)
    private fun validateFileSize(file: MultipartFile) {
        if (file.size > MAX_FILE_SIZE) throw BadRequestException("파일 크기는 5MB 이하만 업로드 가능합니다.")
    }

    // 3. 파일명 유효성 확인 (null, 빈 값, 경로 조작 문자 차단)
    private fun validateFileName(originalFilename: String) {
        if (originalFilename.isBlank()) throw BadRequestException("파일명이 올바르지 않습니다.")

        // 경로 조작 공격 차단: ../ 같은 경로 구분자 포함 여부 확인
        if (originalFilename.any { it == '/' || it == '\\' } || originalFilename.contains("..")) {
            throw BadRequestException("파일명에 허용되지 않는 문자가 포함되어 있습니다.")
        }
    }

    // 4. 확장자 유효성 확인
    private fun validateExtension(originalFilename: String) {
        if (extractExtension(originalFilename) !in ALLOWED_EXTENSIONS) {
            throw BadRequestException("허용되지 않는 파일 형식입니다. 허용 형식: jpg, jpeg, png, gif, webp")
        }
    }

    // 5. MIME 타입 유효성 확인 (확장자 위조 방어)
    private fun validateMimeType(contentType: String) {
        if (contentType.lowercase() !in ALLOWED_MIME_TYPES) {
            throw BadRequestException("허용되지 않는 파일 형식입니다. 이미지 파일만 업로드 가능합니다.")
        }
    }

    // 6. 실제 파일 내용(매직 바이트) 검증
    private fun validateMagicBytes(file: MultipartFile) {
        try {
            val bytes = file.bytes
            if (bytes.size < 4) throw BadRequestException("유효하지 않은 파일입니다.")

            val isValid = isJpeg(bytes) || isPng(bytes) || isGif(bytes) || isWebP(bytes)
            if (!isValid) throw BadRequestException("실제 이미지 파일이 아닙니다.")
        } catch (e: IOException) {
            throw RuntimeException("파일 읽기 실패", e)
        }
    }

    // 확장자 추출 (점 포함, 예: ".png")
    private fun extractExtension(filename: String): String {
        if (!filename.contains(".")) throw BadRequestException("파일 확장자를 확인할 수 없습니다.")
        return filename.substring(filename.lastIndexOf(".")).lowercase()
    }

    // JPEG: FF D8 FF
    private fun isJpeg(bytes: ByteArray) =
        bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()

    // PNG: 89 50 4E 47
    private fun isPng(bytes: ByteArray) =
        bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()

    // GIF: 47 49 46
    private fun isGif(bytes: ByteArray) =
        bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()

    // WebP: RIFF + WEBP (0~3: RIFF, 8~11: WEBP)
    private fun isWebP(bytes: ByteArray) =
        bytes.size >= 12 &&
                bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
}