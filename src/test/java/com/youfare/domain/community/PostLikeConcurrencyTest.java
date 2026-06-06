package com.youfare.domain.community;

import com.youfare.domain.user.Provider;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좋아요 동시성 테스트
 *
 * 여러 유저가 같은 게시글에 동시에 좋아요를 눌러도
 * 낙관적 락(@Version) + 재시도(최대 3회) 덕분에 Post.likeCount가 정확히 집계되는지 검증한다.
 *
 * 핵심 불변식: likeCount == 실제 저장된 PostLike 행 수.
 * (락이 없으면 Lost Update로 likeCount가 행 수보다 작아진다)
 */
@SpringBootTest
@ActiveProfiles("test")
class PostLikeConcurrencyTest {

    @Autowired PostLikeService postLikeService;
    @Autowired PostRepository postRepository;
    @Autowired PostLikeRepository postLikeRepository;
    @Autowired UserRepository userRepository;

    private static final int USER_COUNT = 10;

    private Post post;
    private List<User> users;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User author = userRepository.save(User.builder()
                .socialId("post-author-" + ts)
                .provider(Provider.KAKAO)
                .nickname("작성자")
                .build());

        post = postRepository.save(Post.builder()
                .author(author)
                .category(PostCategory.FREE)
                .title("동시성 테스트 글")
                .content("여러 명이 동시에 좋아요를 누른다")
                .build());

        users = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            users.add(userRepository.save(User.builder()
                    .socialId("liker-" + ts + "-" + i)
                    .provider(Provider.KAKAO)
                    .nickname("유저" + i)
                    .build()));
        }
    }

    @Test
    @DisplayName("10명이 동시에 좋아요 → Lost Update 없이 likeCount == 실제 좋아요 수")
    void concurrentLike_noLostUpdate() throws InterruptedException {
        AtomicInteger success = runConcurrentLikes(USER_COUNT, USER_COUNT);

        Post result = postRepository.findById(post.getId()).orElseThrow();
        long likeRows = postLikeRepository.countByPostId(post.getId());

        // 핵심 불변식: likeCount == 실제 저장된 좋아요 행 수 == 성공한 토글 수.
        // 낙관적 락이 없으면 Lost Update로 likeCount가 행 수보다 작아진다(이 단언이 깨진다).
        assertThat((long) result.getLikeCount()).isEqualTo(likeRows);
        assertThat(result.getLikeCount()).isEqualTo(success.get());
        assertThat(result.getLikeCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("동시 충돌이 재시도(최대 3회) 안에서 흡수되어 전원 정상 반영")
    void concurrentLike_retryRecoversAllConflicts() throws InterruptedException {
        // 동시 인원을 3명으로 두면 최악의 경우라도 한 스레드가 필요한 재시도는 2회뿐 → 3회 cap 안에서 반드시 성공.
        int n = 3;
        AtomicInteger success = runConcurrentLikes(n, n);

        Post result = postRepository.findById(post.getId()).orElseThrow();
        assertThat(success.get()).isEqualTo(n);              // 전원 성공
        assertThat(result.getLikeCount()).isEqualTo(n);      // likeCount 정확히 반영
    }

    /** 앞에서부터 likerCount 명이 동시에(start 래치로 정렬) 같은 글에 좋아요를 누른다. 성공 횟수를 반환. */
    private AtomicInteger runConcurrentLikes(int poolSize, int likerCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(likerCount);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < likerCount; i++) {
            final Long userId = users.get(i).getId();
            executor.submit(() -> {
                try {
                    start.await();                  // 모든 스레드가 동시에 출발
                    postLikeService.toggleLike(userId, post.getId());
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 재시도까지 모두 실패한 경우 (정확성 불변식은 호출부에서 검증)
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();                          // 동시 출발
        done.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        return success;
    }
}
