package com.youfare.external.publicdata;

import com.youfare.domain.welfare.WelfareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 복지 데이터가 한 건도 없으면 공공데이터를 자동으로 1회 시드한다.
 *
 * <p>기존에는 스케줄러(매일 03시)만 있어 신규 DB로 처음 띄우면 다음 03시까지
 * 복지 목록이 빈 채로 보였다(수동 /admin/welfare/sync 호출 필요).
 * 이 시더가 "데이터가 0건일 때만" 동기화를 돌려 그 문제를 막는다.
 *
 * <p>외부 API 호출이 수십 초 걸릴 수 있어 기동을 막지 않도록 별도 데몬 스레드에서 실행한다.
 * 이미 데이터가 있으면 아무것도 하지 않으므로 매 기동마다 API를 호출하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareDataSeeder implements ApplicationRunner {

    private final WelfareRepository welfareRepository;
    private final PublicDataSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        long count = welfareRepository.count();
        if (count > 0) {
            log.info("[시더] 복지 데이터 {}건 존재 — 자동 시드 건너뜀", count);
            return;
        }

        log.info("[시더] 복지 데이터가 비어 있어 공공데이터 자동 시드를 시작합니다 (백그라운드)");
        Thread seeder = new Thread(() -> {
            try {
                int saved = syncService.syncAll();
                log.info("[시더] 자동 시드 완료 — {}건", saved);
            } catch (Exception e) {
                log.error("[시더] 자동 시드 실패: {}", e.getMessage(), e);
            }
        }, "welfare-data-seeder");
        seeder.setDaemon(true);
        seeder.start();
    }
}
