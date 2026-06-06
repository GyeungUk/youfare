package com.youfare.global.storage;

/**
 * 저장 결과.
 *
 * @param key 저장소 내부 키 (예: posts/uuid.png). 삭제 시 사용하므로 DB에 함께 보관한다.
 * @param url 외부에서 이미지를 불러올 수 있는 절대 URL.
 */
public record StoredFile(String key, String url) {
}
