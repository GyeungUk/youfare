package com.youfare.global.storage;

/**
 * 파일 저장 추상화.
 *
 * <p>업로드 "정책(검증)"과 "저장 위치(인프라)"를 분리하기 위한 핵심 인터페이스다.
 * 검증을 통과한 바이트만 받아 저장하고, 외부에서 접근 가능한 URL을 돌려준다.
 *
 * <p>구현체:
 * <ul>
 *   <li>{@link LocalFileStorage} — 개발/테스트용. 로컬 디스크에 저장 + 정적 서빙(/uploads).</li>
 *   <li>{@link S3FileStorage} — 배포용. S3 호환 오브젝트 스토리지(AWS S3 / Cloudflare R2 / MinIO).</li>
 * </ul>
 * 구현체 교체는 {@code storage.type} 설정값 하나로 결정된다({@link StorageConfig}).
 */
public interface FileStorage {

    /**
     * 검증을 마친 이미지 바이트를 저장한다.
     *
     * @param content     파일 바이트 (이미 용량/타입 검증 완료)
     * @param contentType 매직넘버로 판별된 MIME 타입 (예: image/png)
     * @param extension   저장 시 사용할 확장자 (예: png)
     * @return 저장 키(삭제용)와 외부 접근 URL
     */
    StoredFile store(byte[] content, String contentType, String extension);

    /**
     * 저장 키로 파일을 삭제한다. 게시글 삭제 시 연결된 이미지 정리에 사용한다.
     * 이미 없거나 실패해도 예외를 던지지 않는다(best-effort).
     */
    void delete(String storageKey);
}
