package com.youfare.domain.community;

import com.youfare.domain.community.dto.LikeResponse;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 *  좋아요 동시성 처리 — 낙관적 락(Optimistic Lock) + 재시도 전략
 * ============================================================================
 *
 *  [문제]
 *  여러 유저가 같은 게시글에 동시에 좋아요를 누르면 Post.likeCount 갱신이
 *  "읽기 → +1 → 쓰기"로 진행되는 동안 race condition(갱신 유실)이 발생한다.
 *  예) 두 트랜잭션이 동시에 likeCount=10을 읽고 각자 11로 저장 → 결과 11 (실제론 12여야 함).
 *
 *  [선택: 낙관적 락 (@Version on Post)]
 *  - 좋아요는 충돌 빈도가 낮고(같은 글·같은 순간에 누르는 경우는 드묾)
 *    재시도 비용이 적어 낙관적 락이 적합하다.
 *  - 비관적 락(SELECT ... FOR UPDATE)은 DB 행 락 점유 시간이 길어
 *    동시 요청이 모두 줄 서서 기다리게 되므로 처리량(throughput)이 낮아질 수 있다.
 *  - 따라서 평소엔 락 없이 빠르게 처리하고, 드물게 충돌이 나면 그때만 재시도한다.
 *    → "낙관적": "어차피 충돌은 거의 안 날 것"이라고 낙관하고 일단 진행하는 전략.
 *
 *  [동작]
 *  Post의 @Version 컬럼이 커밋 시점에 버전 일치를 검증한다.
 *  다른 트랜잭션이 먼저 likeCount를 갱신해 버전이 어긋나면
 *  ObjectOptimisticLockingFailureException이 발생하고, 이를 잡아 최대 3회 재시도한다.
 *  재시도는 반드시 트랜잭션 "밖"에서 일어나야 매번 새 트랜잭션으로 최신 버전을 다시 읽는다
 *  (그래서 실제 DB 작업은 별도 빈 PostLikeProcessor의 @Transactional 메서드에 위임).
 *
 *  [중복 좋아요 방어]
 *  PostLike의 (post_id, user_id) unique 제약이 "한 유저가 같은 글에 좋아요 2개"를
 *  DB 레벨에서 막는 최종 방어선이다(동시 더블클릭 등).
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {

    /** 낙관적 락 충돌 시 최대 재시도 횟수 */
    private static final int MAX_RETRY = 3;

    private final PostLikeProcessor postLikeProcessor;

    /**
     * 좋아요 토글.
     * NOTE: 이 메서드에는 @Transactional을 붙이지 않는다.
     *       재시도 때마다 새 트랜잭션이 열려 Post의 최신 @Version을 다시 읽어야 하기 때문이다.
     */
    public LikeResponse toggleLike(Long userId, Long postId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return postLikeProcessor.toggleOnce(userId, postId);
            } catch (ObjectOptimisticLockingFailureException e) {
                // 다른 트랜잭션이 먼저 likeCount를 갱신해 버전 충돌 → 재시도
                log.warn("좋아요 낙관적 락 충돌 (postId={}, attempt={}/{})", postId, attempt, MAX_RETRY);
                if (attempt == MAX_RETRY) {
                    throw new BusinessException(ErrorCode.LIKE_CONFLICT);
                }
                backoff(attempt);
            } catch (DataIntegrityViolationException e) {
                // unique 제약 위반: 동시 더블클릭으로 같은 유저의 좋아요가 중복 insert된 경우.
                // 한 번 더 시도하면 "이미 좋아요" 상태로 인식해 정상 토글된다.
                log.warn("좋아요 unique 제약 충돌 (postId={}, userId={}) → 재시도", postId, userId);
                if (attempt == MAX_RETRY) {
                    throw new BusinessException(ErrorCode.LIKE_CONFLICT);
                }
                backoff(attempt);
            }
        }
        // 도달하지 않음 (루프 내에서 반환 또는 예외 발생)
        throw new BusinessException(ErrorCode.LIKE_CONFLICT);
    }

    /** 충돌 직후 즉시 재시도 시 또 부딪치지 않도록 짧게 양보 (지수적 백오프) */
    private void backoff(int attempt) {
        try {
            Thread.sleep(10L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
