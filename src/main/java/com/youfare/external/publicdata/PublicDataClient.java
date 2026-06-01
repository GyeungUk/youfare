package com.youfare.external.publicdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PublicDataClient {

    private final WebClient publicDataWebClient;
    private final String apiKey;

    // baseUrl이 설정된 싱글톤 WebClient 빈을 주입받아 재사용 (매 요청 재생성 방지)
    public PublicDataClient(WebClient publicDataWebClient,
                            @Value("${public-data.api-key}") String apiKey) {
        this.publicDataWebClient = publicDataWebClient;
        this.apiKey = apiKey;
    }

    /**
     * 공공데이터포털 복지서비스 목록 조회
     * @param pageNo 페이지 번호 (1부터)
     * @param numOfRows 페이지당 건수
     */
    public List<PublicWelfareItem> fetchWelfareList(int pageNo, int numOfRows) {
        try {
            PublicWelfareResponse response = publicDataWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            // baseUrl 위에 쿼리 파라미터만 덧붙임 (직접 host/path 파싱 불필요)
                            .queryParam("serviceKey", apiKey)
                            .queryParam("callTp", "L")
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("srchKeyCode", "003") // 청년 대상
                            .build())
                    .retrieve()
                    .bodyToMono(PublicWelfareResponse.class)
                    .block();

            if (response == null || response.getWlfareInfoList() == null) {
                log.warn("공공데이터 API 응답이 비어있습니다. pageNo={}", pageNo);
                return Collections.emptyList();
            }
            return response.getWlfareInfoList();

        } catch (Exception e) {
            // 외부 API 실패 시 예외 격리 — 전체 작업이 죽지 않게 로깅만
            log.error("공공데이터 API 호출 실패: pageNo={}, error={}", pageNo, e.getMessage());
            return Collections.emptyList();
        }
    }
}
