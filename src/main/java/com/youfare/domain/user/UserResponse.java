package com.youfare.domain.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private Integer birthYear;
    private String region;
    private IncomeBracket incomeBracket;
    private EmploymentStatus employmentStatus;
    private Integer point;
    private String grade;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .birthYear(user.getBirthYear())
                .region(user.getRegion())
                .incomeBracket(user.getIncomeBracket())
                .employmentStatus(user.getEmploymentStatus())
                .point(user.getPoint())
                .grade(GradeCalculator.calculate(user.getPoint()))
                .build();
    }
}
