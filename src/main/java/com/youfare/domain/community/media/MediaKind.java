package com.youfare.domain.community.media;

/**
 * 첨부물의 표시 종류. 프런트가 이 값만 보고 렌더링 방식을 정한다.
 * <ul>
 *   <li>{@link #IMAGE} — &lt;img&gt;로 인라인 표시</li>
 *   <li>{@link #VIDEO} — &lt;video controls&gt;로 인라인 재생</li>
 *   <li>{@link #FILE}  — 다운로드 링크(원본 파일명 표시)</li>
 * </ul>
 */
public enum MediaKind {
    IMAGE, VIDEO, FILE
}
