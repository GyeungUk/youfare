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

    /** D-7 이내 마감 임박 항목에만 표시 (null이면 여유 있음) */
    private Long dDay;

    public static WelfareResponse from(Welfare w) {
        Long dDay = null;
        if (w.getApplyEndDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), w.getApplyEndDate());
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
                .incomeCondition(w.getIncomeCondition())
                .applyStartDate(w.getApplyStartDate())
                .applyEndDate(w.getApplyEndDate())
                .sourceUrl(w.getSourceUrl())
                .dDay(dDay)
                .build();
    }
}
