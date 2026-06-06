package com.youfare.domain.welfare;

import java.time.LocalDate;

/**
 * 복지 혜택의 신청 진행 상태.
 * 신청 시작일/마감일을 기준으로 계산한다(날짜가 없는 상시모집은 ONGOING).
 */
public enum WelfareStatus {
    /** 신청 시작 전 */
    UPCOMING,
    /** 신청 진행 중(상시모집 포함) */
    ONGOING,
    /** 신청 마감 */
    CLOSED;

    public static WelfareStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (endDate != null && endDate.isBefore(today)) {
            return CLOSED;
        }
        if (startDate != null && startDate.isAfter(today)) {
            return UPCOMING;
        }
        return ONGOING;
    }
}
