package com.youfare.global.config;

import com.youfare.global.storage.FileStorage;
import com.youfare.global.storage.LocalFileStorage;
import com.youfare.global.storage.S3FileStorage;
import com.youfare.global.storage.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 스토리지 구현체 선택 — {@code storage.type} 설정값에 따라 단 하나의 {@link FileStorage} 빈만 등록한다.
 *
 * <ul>
 *   <li>local(기본) → {@link LocalFileStorage}</li>
 *   <li>s3 → {@link S3FileStorage}</li>
 * </ul>
 * S3 빈은 조건이 맞을 때만 생성되므로, 로컬 개발/테스트에선 S3 자격증명이 없어도 무방하다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(StorageProperties props) {
        return new LocalFileStorage(props.getLocal());
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public FileStorage s3FileStorage(StorageProperties props) {
        return new S3FileStorage(props.getS3());
    }
}
