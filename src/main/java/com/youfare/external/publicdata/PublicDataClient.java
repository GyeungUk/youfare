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

    public PublicDataClient(WebClient publicDataWebClient,
                            @Value("${public-data.api-key}") String apiKey) {
        this.publicDataWebClient = publicDataWebClient;
        this.apiKey = apiKey;
    }

    /**
     * 온통청년(youthcenter.go.kr) 청년정책 목록 조회 (getPlcy)
     * @param pageIndex 페이지 번호 (1부터)
     * @param display   페이지당 건수
     */
    public List<PublicWelfareItem> fetchWelfareList(int pageIndex, int display) {
        try {
            PublicWelfareResponse response = publicDataWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("apiKeyNm", apiKey)
                            .queryParam("pageNum", pageIndex)
                            .queryParam("pageSize", display)
                            .queryParam("rtnType", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(PublicWelfareResponse.class)
                    .block();

            if (response == null || response.getResult() == null
                    || response.getResult().getYouthPolicyList() == null) {
                log.warn("온통청년 API 응답이 비어있습니다. pageIndex={}", pageIndex);
                return Collections.emptyList();
            }
            return response.getResult().getYouthPolicyList();

        } catch (Exception e) {
            log.error("온통청년 API 호출 실패: pageIndex={}, error={}", pageIndex, e.getMessage());
            return Collections.emptyList();
        }
    }
}
