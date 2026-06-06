package com.youfare.domain.community.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.youfare.domain.community.Post;
import com.youfare.domain.community.PostCategory;
import com.youfare.domain.welfare.WelfareResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 응답.
 * relatedWelfare가 있으면 혜택 요약(WelfareResponse)을 함께 내려준다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {

    private Long id;
    private PostCategory category;
    private String title;
    private String content;
    private String authorNickname;
    private boolean anonymous;
    private int viewCount;
    private int likeCount;
    private long commentCount;
    private boolean liked;   // 현재 로그인 유저의 좋아요 여부
    private boolean mine;    // 현재 로그인 유저가 작성자인지
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 첨부물 목록 (이미지·동영상·문서, 없으면 빈 배열) */
    private List<AttachmentResponse> attachments;

    /** 연결된 복지 혜택 요약 (없으면 null → 응답에서 생략) */
    private WelfareResponse relatedWelfare;

    public static PostResponse of(Post post, long commentCount, boolean liked, Long currentUserId) {
        WelfareResponse welfare = post.getRelatedWelfare() != null
                ? WelfareResponse.from(post.getRelatedWelfare())
                : null;

        return PostResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .authorNickname(NicknameMasker.mask(post.getAuthor(), post.isAnonymous()))
                .anonymous(post.isAnonymous())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(commentCount)
                .liked(liked)
                .mine(post.isOwnedBy(currentUserId))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .attachments(post.getAttachments().stream().map(AttachmentResponse::from).toList())
                .relatedWelfare(welfare)
                .build();
    }
}
