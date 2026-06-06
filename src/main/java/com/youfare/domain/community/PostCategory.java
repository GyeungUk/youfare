package com.youfare.domain.community;

/**
 * 게시글 카테고리
 * - REVIEW: 혜택 후기. 특정 Welfare에 연결되어 "이 혜택 후기 N개" 섹션에 노출됨.
 */
public enum PostCategory {
    FREE,      // 자유
    QUESTION,  // 질문
    REVIEW,    // 혜택 후기
    TIP,       // 꿀팁
    ETC        // 기타
}
