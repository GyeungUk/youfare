package com.youfare.global.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 스토리지 설정. application.yml의 {@code storage.*} 값을 바인딩한다.
 *
 * <p>{@code type=local}(기본) 이면 로컬 디스크, {@code type=s3}면 S3 호환 스토리지를 쓴다.
 * 핵심 검증 로직은 스토리지 종류와 무관하므로, 환경마다 이 값만 바꾸면 된다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** local | s3 */
    private String type = "local";

    private Local local = new Local();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        /** 파일이 저장될 로컬 디렉터리 (정적 서빙 루트와 동일). */
        private String baseDir = "./uploads";
        /** 저장된 파일에 접근할 때 쓰는 URL 접두어. baseDir이 /uploads로 서빙된다. */
        private String baseUrl = "http://localhost:8080/uploads";
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region = "ap-northeast-2";
        /** AWS가 아닌 S3 호환 스토리지(R2/MinIO)용 엔드포인트. 비우면 AWS 기본 엔드포인트. */
        private String endpoint;
        private String accessKey;
        private String secretKey;
        /** 공개 접근 URL 접두어(CDN/커스텀 도메인/r2.dev). 비우면 AWS virtual-hosted URL을 생성. */
        private String publicBaseUrl;
    }
}
