package com.youfare.domain.community;

import com.youfare.domain.community.dto.LikeResponse;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 토글의 "단일 시도"를 하나의 트랜잭션으로 실행하는 컴포넌트.
 *
 * <p>재시도 루프(PostLikeService)와 트랜잭션 실행(여기)을 굳이 분리한 이유:
 * Spring의 @Transactional은 프록시 기반이라 "같은 빈 안에서의 self-invocation"에는 적용되지 않는다.
 * 즉 PostLikeService가 자기 자신의 @Transactional 메서드를 호출하면 새 트랜잭션이 열리지 않아,
 * 충돌 후 재시도해도 같은 트랜잭션·같은(낡은) 버전을 그대로 쓰게 된다.
 * 그래서 매 재시도마다 "다른 빈"의 메서드를 호출해 프록시 경계를 넘게 하고,
 * 새 트랜잭션에서 Post의 최신 @Version을 다시 읽도록 만든다.
 */
@Component
@RequiredArgsConstructor
public class PostLikeProcessor {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요가 없으면 추가(+1), 있으면 취소(-1).
     * Post.likeCount를 변경하므로 커밋 시 @Version 검증이 일어나고,
     * 그 사이 다른 트랜잭션이 먼저 커밋했다면 ObjectOptimisticLockingFailureException이 발생한다.
     */
    @Transactional
    public LikeResponse toggleOnce(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return postLikeRepository.findByPostAndUser(post, user)
                .map(existing -> {
                    // 이미 좋아요 → 취소
                    postLikeRepository.delete(existing);
                    post.decreaseLikeCount();
                    return LikeResponse.of(false, post.getLikeCount());
                })
                .orElseGet(() -> {
                    // 좋아요 추가
                    postLikeRepository.save(PostLike.of(post, user));
                    post.increaseLikeCount();
                    return LikeResponse.of(true, post.getLikeCount());
                });
    }
}
