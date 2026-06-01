package com.youfare.domain.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OnboardingRequest {

    // @Max 하드코딩 제거 → 서비스 계층에서 LocalDate.now().getYear() 기준으로 동적 검증
    @NotNull(message = "출생연도는 필수입니다.")
    @Min(value = 1900, message = "유효하지 않은 출생연도입니다.")
    private Integer birthYear;

    @NotBlank(message = "지역은 필수입니다.")
    private String region;

    @NotNull(message = "소득구간은 필수입니다.")
    private IncomeBracket incomeBracket;

    @NotNull(message = "취업상태는 필수입니다.")
    private EmploymentStatus employmentStatus;
}
