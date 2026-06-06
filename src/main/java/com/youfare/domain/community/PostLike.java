package com.youfare.domain.community;

import com.youfare.domain.user.User;
import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 좋아요.
 * (post_id, user_id) 복합 unique 제약으로 "한 유저가 같은 글에 좋아요 1개"를 DB 레벨에서 보장한다.
 * → 동시 더블클릭 같은 중복 좋아요는 unique 제약이 최종 방어선.
 */
@Entity
@Table(
    name = "post_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static PostLike of(Post post, User user) {
        return PostLike.builder()
                .post(post)
                .user(user)
                .build();
    }
}
