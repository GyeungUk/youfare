package com.youfare.domain.scrap;

import com.youfare.domain.welfare.WelfareResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScrapResponse {
    private Long scrapId;
    private WelfareResponse welfare;
    private LocalDateTime scrappedAt;

    public static ScrapResponse from(Scrap scrap) {
        return ScrapResponse.builder()
                .scrapId(scrap.getId())
                .welfare(WelfareResponse.from(scrap.getWelfare()))
                .scrappedAt(scrap.getCreatedAt())
                .build();
    }
}
