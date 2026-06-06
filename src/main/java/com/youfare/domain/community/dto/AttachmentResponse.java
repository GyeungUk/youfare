package com.youfare.domain.community.dto;

import com.youfare.domain.community.PostAttachment;
import com.youfare.domain.community.media.MediaKind;

/**
 * 게시글 첨부물 응답 한 건.
 *
 * @param url         외부 접근 URL(표시/다운로드)
 * @param kind        렌더링 종류(IMAGE·VIDEO·FILE) — 프런트가 이 값으로 표시 방식을 정한다
 * @param name        원본 파일명(파일 다운로드 라벨)
 * @param contentType MIME 타입
 */
public record AttachmentResponse(String url, MediaKind kind, String name, String contentType) {

    public static AttachmentResponse from(PostAttachment a) {
        return new AttachmentResponse(a.getUrl(), a.getKind(), a.getOriginalName(), a.getContentType());
    }
}
