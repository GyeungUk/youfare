package com.youfare.domain.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 조회수 +1 — 원자적 UPDATE.
     * 엔티티를 더럽히지 않아 @Version 검증을 건드리지 않으므로,
     * 같은 글을 동시에 조회해도 낙관적 락 충돌(500)이 발생하지 않고 정확히 누적된다.
     */
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /**
     * 목록 조회 — 카테고리 필터(선택) + 페이징.
     * 정렬(최신순/좋아요순)은 Pageable의 Sort로 전달된다.
     * author를 JOIN FETCH해 작성자 닉네임 조회 시 N+1을 방지한다.
     * (ToOne fetch join은 페이징과 함께 써도 in-memory 페이징 경고가 발생하지 않음)
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author " +
                   "WHERE (:category IS NULL OR p.category = :category)",
           countQuery = "SELECT COUNT(p) FROM Post p " +
                        "WHERE (:category IS NULL OR p.category = :category)")
    Page<Post> findByFilter(@Param("category") PostCategory category, Pageable pageable);

    /**
     * 인기글 — 최근 7일(since 이후) 작성된 글 중 인기 점수 내림차순.
     * 인기 점수 = likeCount * 2 + viewCount (가중치를 쿼리에서 직접 계산).
     * 상위 N개는 Pageable(PageRequest.of(0, 10))로 제한한다.
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.author " +
           "WHERE p.createdAt >= :since " +
           "ORDER BY (p.likeCount * 2 + p.viewCount) DESC, p.createdAt DESC")
    List<Post> findHotPosts(@Param("since") LocalDateTime since, Pageable pageable);

    /** 특정 복지 혜택에 연결된 후기 글 목록 (혜택 상세 페이지의 "후기 N개" 섹션용) */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author " +
                   "WHERE p.relatedWelfare.id = :welfareId AND p.category = :category",
           countQuery = "SELECT COUNT(p) FROM Post p " +
                        "WHERE p.relatedWelfare.id = :welfareId AND p.category = :category")
    Page<Post> findByRelatedWelfareIdAndCategory(@Param("welfareId") Long welfareId,
                                                 @Param("category") PostCategory category,
                                                 Pageable pageable);

    /** 내가 쓴 글 목록 (마이페이지용) */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author WHERE p.author.id = :authorId",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId")
    Page<Post> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);
}
