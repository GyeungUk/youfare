package com.youfare.domain.community.dto;

import com.youfare.domain.user.User;

/**
 * 작성자 닉네임 마스킹 헬퍼.
 * - 익명 글이면 "익명 청년"으로 마스킹
 * - 소셜 로그인 특성상 닉네임이 비어 있을 수 있어 기본값 "청년"으로 대체
 */
final class NicknameMasker {

    private NicknameMasker() {
    }

    static String mask(User author, boolean anonymous) {
        if (anonymous) {
            return "익명 청년";
        }
        if (author == null || author.getNickname() == null || author.getNickname().isBlank()) {
            return "청년";
        }
        return author.getNickname();
    }
}
