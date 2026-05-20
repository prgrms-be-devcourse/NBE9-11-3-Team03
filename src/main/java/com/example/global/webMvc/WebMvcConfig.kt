package com.example.global.webMvc

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebMvcConfig(
    // application.yaml에 설정한 경로를 가져옵니다.
    @Value("\${file.upload-dir}")
    private val uploadDir: String
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }

    // 로컬 저장소의 파일을 외부에서 URL로 접근 가능하게 매핑합니다.
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // OS 환경에 맞춰 안전한 절대 경로 URI (file:///...) 형식으로 자동 변환합니다.
        val uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString()
        //  리소스 핸들러 등록
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath)
    }
}