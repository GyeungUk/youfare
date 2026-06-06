package com.youfare.global.storage;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 디스크 저장 구현 (개발/테스트용).
 *
 * <p>baseDir 아래 {@code posts/{uuid}.{ext}} 형태로 저장하고, baseUrl을 붙여 접근 URL을 만든다.
 * 저장된 파일은 {@code WebMvcConfig}의 정적 리소스 핸들러(/uploads/**)로 서빙된다.
 *
 * <p>자격증명이 전혀 필요 없어 로컬에서 전체 업로드 플로우를 바로 검증할 수 있다.
 * 단, 컨테이너 배포 환경은 디스크가 휘발성이라 운영에선 {@link S3FileStorage}를 쓴다.
 */
@Slf4j
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;
    private final String baseUrl;

    public LocalFileStorage(StorageProperties.Local props) {
        this.baseDir = Paths.get(props.getBaseDir()).toAbsolutePath().normalize();
        this.baseUrl = stripTrailingSlash(props.getBaseUrl());
    }

    @Override
    public StoredFile store(byte[] content, String contentType, String extension) {
        String key = "posts/" + UUID.randomUUID() + "." + extension;
        Path target = baseDir.resolve(key).normalize();

        // 경로 탐색(path traversal) 방어: 최종 경로가 baseDir를 벗어나면 거부.
        // (key는 서버가 UUID로 생성하므로 실제 위험은 없지만, 방어선을 한 겹 더 둔다.)
        if (!target.startsWith(baseDir)) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            log.error("로컬 파일 저장 실패: {}", target, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return new StoredFile(key, baseUrl + "/" + key);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path target = baseDir.resolve(storageKey).normalize();
            if (target.startsWith(baseDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            // 파일 정리는 best-effort — 실패해도 게시글 삭제 흐름을 막지 않는다.
            log.warn("로컬 파일 삭제 실패: {}", storageKey, e);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
