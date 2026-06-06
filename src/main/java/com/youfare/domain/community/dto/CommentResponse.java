package com.youfare.domain.community.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.youfare.domain.community.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 응답.
 * - children: 대댓글 중첩(2depth 고정).
 * - 삭제된 댓글(deleted=true)은 content를 "삭제된 댓글입니다"로 대체하되 구조는 유지.
 * - postId/postTitle: 마이페이지 "내 댓글" 목록에서만 채워지고, 중첩 트리 응답에선 null로 생략됨.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {

    private static final String DELETED_CONTENT = "삭제된 댓글입니다";

    private Long id;
    private String content;
    private String authorNickname;
    private boolean deleted;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentResponse> children = new ArrayList<>();

    // 마이페이지 전용 (중첩 트리에서는 미사용)
    private Long postId;
    private String postTitle;

    /** 중첩 트리 노드 생성 */
    public static CommentResponse of(Comment comment, List<CommentResponse> children) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.isDeleted() ? DELETED_CONTENT : comment.getContent())
                .authorNickname(comment.isDeleted() ? null
                        : NicknameMasker.mask(comment.getAuthor(), false))
                .deleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .children(children)
                .build();
    }

    /** 마이페이지 "내 댓글" 목록용 (글 정보 포함, children 없음) */
    public static CommentResponse forMyPage(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorNickname(NicknameMasker.mask(comment.getAuthor(), false))
                .deleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .children(null)
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .build();
    }
}
