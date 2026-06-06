package com.youfare.domain.community;

import com.youfare.domain.user.User;
import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 대댓글을 위한 self-referencing 부모 댓글.
     * 댓글(부모 없음) → 대댓글(부모 있음)까지 2depth로 고정한다.
     * "부모가 이미 대댓글인 댓글"은 만들 수 없게 Service 레이어에서 검증한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /** soft delete 플래그. true면 조회 시 "삭제된 댓글입니다"로 내용을 대체하되 트리 구조는 유지한다. */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public boolean isReply() {
        return this.parent != null;
    }

    public boolean isOwnedBy(Long userId) {
        return this.author != null && this.author.getId().equals(userId);
    }

    /** soft delete — 실제 삭제 대신 플래그만 변경해 대댓글 트리 구조를 보존 */
    public void softDelete() {
        this.deleted = true;
    }
}
