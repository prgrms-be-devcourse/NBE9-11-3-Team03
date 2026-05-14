package com.example.global.webMvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // application.yaml에 설정한 경로를 가져옵니다.
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // 로컬 저장소의 파일을 외부에서 URL로 접근 가능하게 매핑합니다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. OS 환경에 맞춰 안전한 절대 경로 URI (file:///...) 형식으로 자동 변환합니다.
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();

        // 2. 서버 실행 시 콘솔에 찍어서 실제 경로가 맞는지 눈으로 확인합니다.
        System.out.println("✅ [정적 리소스 매핑 경로] : " + uploadPath);

        // 3. 리소스 핸들러 등록
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}