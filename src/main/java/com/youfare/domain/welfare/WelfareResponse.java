package com.youfare.domain.welfare;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WelfareResponse {

    private Long id;
    private String externalId;
    private String title;
    private WelfareCategory category;
    private String description;
    private Integer targetAgeMin;
    private Integer targetAgeMax;
    private String region;
    private String incomeCondition;
    private LocalDate applyStartDate;
    private LocalDate applyEndDate;
    private String sourceUrl;

    /** 신청 진행 상태 (진행중/마감/예정) */
    private WelfareStatus status;

    /** D-7 이내 마감 임박 항목에만 표시 (null이면 여유 있음) */
    private Long dDay;

    public static WelfareResponse from(Welfare w) {
        LocalDate today = LocalDate.now();
        Long dDay = null;
        if (w.getApplyEndDate() != null) {
            long days = ChronoUnit.DAYS.between(today, w.getApplyEndDate());
            if (days >= 0 && days <= 7) dDay = days;
        }
        return WelfareResponse.builder()
                .id(w.getId())
                .externalId(w.getExternalId())
                .title(w.getTitle())
                .category(w.getCategory())
                .description(w.getDescription())
                .targetAgeMin(w.getTargetAgeMin())
                .targetAgeMax(w.getTargetAgeMax())
                .region(w.getRegion())
                .incomeCondition(sanitizeIncomeCondition(w.getIncomeCondition()))
                .applyStartDate(w.getApplyStartDate())
                .applyEndDate(w.getApplyEndDate())
                .sourceUrl(w.getSourceUrl())
                .status(WelfareStatus.of(w.getApplyStartDate(), w.getApplyEndDate(), today))
                .dDay(dDay)
                .build();
    }

    /**
     * 과거 동기화로 코드값("0043001" 등)이 그대로 저장된 소득 조건을 라벨로 변환.
     * 온통청년 earnCndSeCd 코드: 0043001=제한 없음, 0043002=소득 조건 있음, 0043003=기타.
     * 코드가 아닌 일반 설명 문자열은 그대로 반환한다.
     */
    private static String sanitizeIncomeCondition(String value) {
        if (value == null) return null;
        switch (value.trim()) {
            case "0043001": return "제한 없음";
            case "0043002": return "소득 조건 있음";
            case "0043003": return "기타";
            default: return value;
        }
    }
}
