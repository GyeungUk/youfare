package com.youfare.external.publicdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 공공데이터 동기화 오케스트레이터.
 *
 * 이 클래스 자체에는 트랜잭션을 두지 않는다(외부 API 호출 동안 커넥션 점유 방지).
 * 실제 DB 쓰기는 WelfareUpsertService가 단건 독립 트랜잭션(REQUIRES_NEW)으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataSyncService {

    private static final int PAGE_SIZE = 100;

    private final PublicDataClient publicDataClient;
    private final WelfareUpsertService welfareUpsertService;

    /** 하루 1회 자동 갱신 (매일 오전 3시) */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledSync() {
        log.info("[스케줄러] 공공데이터 복지정보 자동 동기화 시작");
        syncAll();
    }

    /**
     * 전체 데이터 동기화 (페이징 처리)
     * - externalId 기준 이미 있으면 update, 없으면 insert (upsert 전략)
     * - 단건 실패가 전체를 중단시키지 않도록 예외 격리(부분 성공 허용)
     */
    public int syncAll() {
        int page = 1;
        int totalSaved = 0;

        while (true) {
            List<PublicWelfareItem> items = publicDataClient.fetchWelfareList(page, PAGE_SIZE);
            if (items.isEmpty()) break;

            for (PublicWelfareItem item : items) {
                try {
                    welfareUpsertService.upsertOne(item); // 단건 독립 트랜잭션
                    totalSaved++;
                } catch (Exception e) {
                    log.error("복지 항목 저장 실패: externalId={}, error={}", item.getServId(), e.getMessage());
                }
            }

            if (items.size() < PAGE_SIZE) break; // 마지막 페이지
            page++;
        }

        log.info("[동기화 완료] 총 {}건 처리", totalSaved);
        return totalSaved;
    }
}
