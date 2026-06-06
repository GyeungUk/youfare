package com.youfare.domain.community;

import com.youfare.domain.community.dto.CommentRequest;
import com.youfare.domain.community.dto.CommentResponse;
import com.youfare.domain.scrap.PointPolicy;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int LIST_SIZE = 10;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 작성 — parentId 없으면 댓글, 있으면 대댓글.
     * 대댓글의 부모가 이미 대댓글이면(2depth 초과) 400 에러.
     * 성공 시 작성자에게 포인트 +4 적립(비관적 락으로 동시 적립 방지).
     */
    @Transactional
    public CommentResponse create(Long userId, Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        User author = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            // 부모 댓글이 같은 게시글의 것인지 확인
            if (!parent.getPost().getId().equals(postId)) {
                throw new BusinessException(ErrorCode.COMMENT_POST_MISMATCH);
            }
            // 2depth 고정: 부모가 이미 대댓글이면 그 아래로는 달 수 없음
            if (parent.isReply()) {
                throw new BusinessException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }
        }

        Comment comment = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content(request.getContent())
                .parent(parent)
                .build());

        author.addPoint(PointPolicy.COMMENT_CREATE);   // 댓글 작성 +4 포인트

        return CommentResponse.of(comment, new ArrayList<>());
    }

    /**
     * 댓글 목록 조회 — 댓글 + 하위 대댓글 중첩 구조로 반환.
     * 전체 댓글을 한 번에 조회한 뒤 parentId 기준으로 메모리에서 트리를 조립한다(2depth 고정).
     * 삭제된 댓글은 "삭제된 댓글입니다"로 내용만 대체되고 트리 구조는 유지된다.
     */
    public List<CommentResponse> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        List<Comment> all = commentRepository.findByPostIdWithAuthor(postId);

        // 부모 id → 대댓글 응답 리스트 (작성순 유지를 위해 LinkedHashMap)
        Map<Long, List<CommentResponse>> childrenByParentId = new LinkedHashMap<>();
        for (Comment c : all) {
            if (c.isReply()) {
                childrenByParentId
                        .computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>())
                        .add(CommentResponse.of(c, new ArrayList<>()));
            }
        }

        List<CommentResponse> roots = new ArrayList<>();
        for (Comment c : all) {
            if (!c.isReply()) {
                List<CommentResponse> children =
                        childrenByParentId.getOrDefault(c.getId(), new ArrayList<>());
                roots.add(CommentResponse.of(c, children));
            }
        }
        return roots;
    }

    /** 댓글 삭제 — 본인만, soft delete(isDeleted=true)로 트리 구조 보존 */
    @Transactional
    public void delete(Long userId, Long postId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new BusinessException(ErrorCode.COMMENT_POST_MISMATCH);
        }
        if (!comment.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
        }

        // 이미 삭제된 댓글이면 건너뛴다(중복 호출 시 포인트 이중 차감 방지).
        if (!comment.isDeleted()) {
            // 작성 시 적립한 포인트 회수(작성→삭제 반복 어뷰징 차단). 비관적 락 + 0 미만 방지.
            User author = userRepository.findByIdWithLock(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            author.setPoint(Math.max(0, author.getPoint() - PointPolicy.COMMENT_CREATE));
            comment.softDelete();
        }
    }

    /** 내가 쓴 댓글 목록 (마이페이지용) */
    public Page<CommentResponse> getMyComments(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findByAuthorId(userId, pageable)
                .map(CommentResponse::forMyPage);
    }
}
