package com.youfare.global.config;

import com.youfare.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 로컬 스토리지 사용 시, 업로드 디렉터리를 {@code /uploads/**} 경로로 정적 서빙한다.
 *
 * <p>S3 모드에서는 이미지 URL이 오브젝트 스토리지를 직접 가리키므로 이 핸들러가 필요 없다(설정해도 무해).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties props;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(props.getType())) {
            return;
        }
        Path baseDir = Paths.get(props.getLocal().getBaseDir()).toAbsolutePath().normalize();
        // toUri()는 "file:///abs/path/" 형태(끝 슬래시 포함)라 리소스 위치로 바로 쓸 수 있다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(baseDir.toUri().toString());
    }
}
