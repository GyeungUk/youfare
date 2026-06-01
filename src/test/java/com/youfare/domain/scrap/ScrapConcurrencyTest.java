package com.youfare.domain.scrap;

import com.youfare.domain.user.*;
import com.youfare.domain.welfare.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 포인트 동시성 테스트
 * 동시에 여러 요청이 들어와도 비관적 락으로 포인트가 정확히 누적되는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class ScrapConcurrencyTest {

    @Autowired ScrapService scrapService;
    @Autowired UserRepository userRepository;
    @Autowired WelfareRepository welfareRepository;

    private User testUser;
    private Welfare[] welfares;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .socialId("test-concurrency-" + System.currentTimeMillis())
                .provider(Provider.KAKAO)
                .nickname("테스트유저")
                .build());

        welfares = new Welfare[5];
        for (int i = 0; i < 5; i++) {
            welfares[i] = welfareRepository.save(Welfare.builder()
                    .externalId("TEST-CONCURRENT-" + System.currentTimeMillis() + "-" + i)
                    .title("테스트 복지 " + i)
                    .category(WelfareCategory.ETC)
                    .build());
        }
    }

    @Test
    @DisplayName("동시 스크랩 5건 → 포인트 정확히 10 누적")
    void concurrentScrap_pointAccumulatesCorrectly() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long welfareId = welfares[i].getId();
            executor.submit(() -> {
                try {
                    scrapService.addScrap(testUser.getId(), welfareId);
                } catch (Exception e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        User result = userRepository.findById(testUser.getId()).orElseThrow();
        // 스크랩 1건당 +2포인트, 5건 = 10포인트
        assertThat(result.getPoint()).isEqualTo(10);
    }
}
