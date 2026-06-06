package com.youfare.domain.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 전체 댓글(대댓글 포함)을 작성순으로 조회.
     * author를 JOIN FETCH해 닉네임 조회 N+1을 방지하고,
     * 중첩 트리는 Service에서 parentId 기준으로 메모리에서 조립한다.
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
           "WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);

    /** 삭제되지 않은 댓글 수 (게시글 상세의 댓글 개수 표시용) */
    long countByPostIdAndDeletedFalse(Long postId);

    /** 내가 쓴 댓글 목록 (마이페이지용) — 글 제목 표시를 위해 post까지 fetch */
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.post " +
                   "WHERE c.author.id = :authorId AND c.deleted = false " +
                   "ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c " +
                        "WHERE c.author.id = :authorId AND c.deleted = false")
    Page<Comment> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    // 게시글 hard delete 시 연관 댓글 정리.
    // self-FK(parent) 제약 위반을 피하려 대댓글(parent 있음)을 먼저 지우고 부모 댓글을 지운다.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId AND c.parent IS NOT NULL")
    void deleteRepliesByPostId(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
