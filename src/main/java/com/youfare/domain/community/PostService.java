package com.youfare.domain.community;

import com.youfare.domain.community.dto.PostListResponse;
import com.youfare.domain.community.dto.PostRequest;
import com.youfare.domain.community.dto.PostResponse;
import com.youfare.domain.community.dto.PostUpdateRequest;
import com.youfare.domain.community.media.MediaUploadValidator;
import com.youfare.domain.community.media.MediaUploadValidator.ValidatedMedia;
import com.youfare.domain.scrap.PointPolicy;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.domain.welfare.Welfare;
import com.youfare.domain.welfare.WelfareRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import com.youfare.global.storage.FileStorage;
import com.youfare.global.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int LIST_SIZE = 10;
    private static final int HOT_SIZE = 10;
    private static final int HOT_RECENT_DAYS = 7;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final WelfareRepository welfareRepository;
    private final MediaUploadValidator mediaUploadValidator;
    private final FileStorage fileStorage;

    /**
     * 게시글 작성 — 성공 시 작성자에게 포인트 +5 적립.
     * 포인트 누적은 비관적 락(findByIdWithLock)으로 조회해 동시 적립 race condition을 방지
     * (스크랩 포인트 적립과 동일한 전략 재사용).
     *
     * <p>첨부물(이미지·동영상·문서)은 선택 사항이다. 첨부 시 정책(최대 10개·각 20MB·총 50MB)을
     * 먼저 검증한 뒤 저장소에 업로드하고, 게시글과 함께 영속화한다.
     */
    @Transactional
    public PostResponse create(Long userId, PostRequest request, List<MultipartFile> attachments) {
        User author = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Welfare relatedWelfare = null;
        if (request.getRelatedWelfareId() != null) {
            relatedWelfare = welfareRepository.findById(request.getRelatedWelfareId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.WELFARE_NOT_FOUND));
        }

        // 1) 정책 검증 — 통과한 바이트만 받는다(실패 시 저장 전에 예외 → 업로드 안 됨).
        List<ValidatedMedia> validated = mediaUploadValidator.validate(attachments);

        Post post = Post.builder()
                .author(author)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .anonymous(request.isAnonymous())
                .relatedWelfare(relatedWelfare)
                .build();

        // 2) 저장소 업로드 후 게시글에 연결(순서 보존).
        int order = 0;
        for (ValidatedMedia m : validated) {
            StoredFile stored = fileStorage.store(m.bytes(), m.contentType(), m.extension());
            post.addAttachment(m.kind(), stored.key(), stored.url(), m.contentType(), m.originalName(), order++);
        }

        postRepository.save(post);   // cascade로 PostAttachment도 함께 저장
        author.addPoint(PointPolicy.POST_CREATE);   // 게시글 작성 +5 포인트

        return PostResponse.of(post, 0, false, userId);
    }

    /**
     * 목록 조회 — 카테고리 필터 + 페이징(size=10).
     * sort=likes 면 좋아요 내림차순, 그 외(latest 기본)는 최신순.
     */
    public Page<PostListResponse> getList(PostCategory category, String sort, int page) {
        return postRepository.findByFilter(category, listPageable(sort, page))
                .map(PostListResponse::from);
    }

    /** 상세 조회 — 조회수 +1 (쓰기 트랜잭션) */
    @Transactional
    public PostResponse getDetail(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 조회수는 @Version 엔티티를 더럽히지 않는 원자적 UPDATE로 올린다.
        // (기존 post.increaseViewCount()는 같은 글 동시 조회 시 낙관적 락 충돌 → 500을 유발했다.)
        // 응답의 viewCount는 이번 조회 직전 값이며, 다음 조회 시 반영된다.
        postRepository.incrementViewCount(postId);

        long commentCount = commentRepository.countByPostIdAndDeletedFalse(postId);
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, currentUserId);

        return PostResponse.of(post, commentCount, liked, currentUserId);
    }

    /** 게시글 수정 — 본인만 가능, 제목/내용만 변경 */
    @Transactional
    public PostResponse update(Long userId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.POST_FORBIDDEN);
        }

        post.update(request.getTitle(), request.getContent());

        long commentCount = commentRepository.countByPostIdAndDeletedFalse(postId);
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return PostResponse.of(post, commentCount, liked, userId);
    }

    /**
     * 게시글 삭제 — 본인만 가능, hard delete.
     * self-FK(대댓글→댓글) 제약을 피하려 대댓글 → 댓글 → 좋아요 순으로 연관 데이터를 먼저 정리한다.
     */
    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.POST_FORBIDDEN);
        }

        // 작성 시 적립한 포인트를 회수한다(작성→삭제 반복으로 포인트를 무한 적립하는 어뷰징 차단).
        // 동시 적립/회수 race를 막기 위해 작성과 동일하게 비관적 락으로 유저를 조회하고, 0 미만으로는 내리지 않는다.
        User author = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        author.setPoint(Math.max(0, author.getPoint() - PointPolicy.POST_CREATE));

        // 저장소의 실제 파일은 cascade 대상이 아니므로 키를 미리 확보해 직접 정리한다(best-effort).
        List<String> attachmentKeys = post.getAttachmentKeys();

        commentRepository.deleteRepliesByPostId(postId);
        commentRepository.deleteAllByPostId(postId);
        postLikeRepository.deleteAllByPostId(postId);
        postRepository.delete(post);   // PostAttachment 행은 cascade/orphanRemoval로 함께 삭제

        attachmentKeys.forEach(fileStorage::delete);
    }

    /**
     * 인기글 — 최근 7일 내 작성된 글 중 (likeCount * 2 + viewCount) 상위 10개.
     */
    public List<PostListResponse> getHotPosts() {
        LocalDateTime since = LocalDateTime.now().minusDays(HOT_RECENT_DAYS);
        return postRepository.findHotPosts(since, PageRequest.of(0, HOT_SIZE)).stream()
                .map(PostListResponse::from)
                .toList();
    }

    /** 특정 복지 혜택의 후기(REVIEW) 글 목록 */
    public Page<PostListResponse> getWelfareReviews(Long welfareId, int page) {
        Pageable pageable = PageRequest.of(page, LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByRelatedWelfareIdAndCategory(welfareId, PostCategory.REVIEW, pageable)
                .map(PostListResponse::from);
    }

    /** 내가 쓴 글 목록 (마이페이지용) */
    public Page<PostListResponse> getMyPosts(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByAuthorId(userId, pageable)
                .map(PostListResponse::from);
    }

    /** sort 파라미터 → Pageable 변환 (likes: 좋아요 내림차순, 그 외: 최신순) */
    private Pageable listPageable(String sort, int page) {
        Sort sortOption = "likes".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "likeCount").and(Sort.by(Sort.Direction.DESC, "createdAt"))
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(page, LIST_SIZE, sortOption);
    }
}
