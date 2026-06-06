package com.youfare.domain.community;

import com.youfare.domain.community.media.MediaKind;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글에 첨부된 파일 하나(이미지·동영상·문서).
 *
 * <p>{@code url}은 화면 표시·다운로드용 외부 접근 주소, {@code storageKey}는 저장소 키(삭제용)다.
 * {@code kind}로 프런트가 렌더링 방식(이미지/동영상/파일)을 정하고,
 * {@code originalName}은 다운로드 파일로 보여줄 원본 이름이다. 정렬 순서는 {@code sortOrder}로 보존한다.
 */
@Entity
@Table(name = "post_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaKind kind;

    @Column(nullable = false)
    private String url;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
