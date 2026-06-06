package com.youfare.domain.community.dto;

import com.youfare.domain.community.Post;
import com.youfare.domain.community.PostCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 목록용 경량 응답.
 * 본문(content)은 제외하고, 작성자 닉네임은 익명 글이면 "익명 청년"으로 마스킹한다.
 */
@Getter
@Builder
public class PostListResponse {

    private Long id;
    private PostCategory category;
    private String title;
    private String authorNickname;
    private boolean anonymous;
    private int viewCount;
    private int likeCount;
    private LocalDateTime createdAt;

    public static PostListResponse from(Post post) {
        return PostListResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .authorNickname(NicknameMasker.mask(post.getAuthor(), post.isAnonymous()))
                .anonymous(post.isAnonymous())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
