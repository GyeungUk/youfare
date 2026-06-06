package com.youfare.domain.community;

import com.youfare.domain.community.media.MediaKind;
import com.youfare.domain.user.User;
import com.youfare.domain.welfare.Welfare;
import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    /** 혜택 후기(REVIEW)일 때 연결되는 복지 혜택. 일반 글은 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_welfare_id")
    private Welfare relatedWelfare;

    @Builder.Default
    @Column(nullable = false)
    private int viewCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    /**
     * 낙관적 락 버전.
     * 여러 유저가 동시에 좋아요를 누르면 likeCount 갱신이 race condition을 일으킨다.
     * @Version 컬럼을 두면 트랜잭션 커밋 시점에 버전이 일치하는지 검증하고,
     * 다른 트랜잭션이 먼저 갱신해 버전이 어긋나면 ObjectOptimisticLockingFailureException을 던진다.
     * (실제 재시도 처리는 PostLikeService 참고)
     */
    @Version
    private Long version;

    /**
     * 첨부물 (이미지·동영상·문서, 최대 10개, 정렬 순서 보존).
     * cascade ALL + orphanRemoval: 게시글 저장/삭제 시 첨부 행도 함께 처리된다.
     * (저장소의 실제 파일 삭제는 PostService가 storageKey로 별도 수행)
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    @Builder.Default
    private List<PostAttachment> attachments = new ArrayList<>();

    /** 게시글 수정 — 본인만 호출(권한 검증은 Service 레이어) */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /** 검증·저장을 마친 첨부물을 연결한다(연관관계 편의 메서드). */
    public void addAttachment(MediaKind kind, String storageKey, String url,
                              String contentType, String originalName, int sortOrder) {
        this.attachments.add(PostAttachment.builder()
                .post(this)
                .kind(kind)
                .storageKey(storageKey)
                .url(url)
                .contentType(contentType)
                .originalName(originalName)
                .sortOrder(sortOrder)
                .build());
    }

    /** 저장소 파일 삭제용 키 목록 */
    public List<String> getAttachmentKeys() {
        return this.attachments.stream().map(PostAttachment::getStorageKey).toList();
    }

    /** 상세 조회 시 조회수 +1 */
    public void increaseViewCount() {
        this.viewCount += 1;
    }

    public void increaseLikeCount() {
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        // 좋아요 수가 음수가 되지 않도록 방어
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public boolean isOwnedBy(Long userId) {
        return this.author != null && this.author.getId().equals(userId);
    }
}
