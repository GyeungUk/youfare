package com.youfare.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

/** 좋아요 토글 결과 — 현재 좋아요 여부와 최종 좋아요 수 */
@Getter
@Builder
public class LikeResponse {

    private boolean liked;
    private int likeCount;

    public static LikeResponse of(boolean liked, int likeCount) {
        return LikeResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
