package com.youfare.global.storage;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

/**
 * S3 호환 오브젝트 스토리지 저장 구현 (배포용).
 *
 * <p>AWS SDK v2의 {@link S3Client}는 엔드포인트만 바꾸면 AWS S3 / Cloudflare R2 / MinIO를
 * 동일 코드로 다룰 수 있다. 그래서 특정 벤더에 묶지 않고 endpoint/credential을 설정값으로 받는다.
 *
 * <p>업로드는 {@code 클라이언트 → 서버(검증) → S3} 순서다(presigned URL 직접 업로드 X).
 * 검증(타입/용량/장수)을 서버에서 강제해야 하므로, 파일은 반드시 서버를 경유한다.
 *
 * <p>HTTP 클라이언트로는 JDK 내장 {@link UrlConnectionHttpClient}를 써서 Netty/Apache 의존성 무게를 피한다.
 */
@Slf4j
public class S3FileStorage implements FileStorage {

    private final S3Client client;
    private final StorageProperties.S3 props;

    public S3FileStorage(StorageProperties.S3 props) {
        this.props = props;

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
                .httpClient(UrlConnectionHttpClient.builder().build());

        // AWS가 아닌 S3 호환 스토리지(R2/MinIO)는 엔드포인트를 직접 지정.
        // 커스텀 엔드포인트는 가상호스트(virtual-host) 대신 path-style 접근이 안전하다.
        if (StringUtils.hasText(props.getEndpoint())) {
            builder.endpointOverride(URI.create(props.getEndpoint()))
                    .forcePathStyle(true);
        }

        this.client = builder.build();
    }

    @Override
    public StoredFile store(byte[] content, String contentType, String extension) {
        String key = "posts/" + UUID.randomUUID() + "." + extension;
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (Exception e) {
            log.error("S3 업로드 실패: bucket={}, key={}", props.getBucket(), key, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return new StoredFile(key, publicUrl(key));
    }

    @Override
    public void delete(String storageKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(storageKey)
                    .build());
        } catch (Exception e) {
            // best-effort — 실패해도 게시글 삭제 흐름을 막지 않는다.
            log.warn("S3 파일 삭제 실패: key={}", storageKey, e);
        }
    }

    /**
     * 공개 URL 생성.
     * publicBaseUrl(CDN/커스텀 도메인/r2.dev)이 있으면 그것을 쓰고,
     * 없으면 AWS virtual-hosted 형식으로 만든다.
     */
    private String publicUrl(String key) {
        if (StringUtils.hasText(props.getPublicBaseUrl())) {
            String base = props.getPublicBaseUrl();
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            return base + "/" + key;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                props.getBucket(), props.getRegion(), key);
    }
}
