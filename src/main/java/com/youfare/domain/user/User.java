package com.youfare.domain.user;

import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    private String email;

    private String nickname;

    private Integer birthYear;

    private String region;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IncomeBracket incomeBracket = IncomeBracket.UNKNOWN;

    @Enumerated(EnumType.STRING)
    private EmploymentStatus employmentStatus;

    @Builder.Default
    private Integer point = 0;

    public void updateOnboarding(Integer birthYear, String region,
                                 IncomeBracket incomeBracket, EmploymentStatus employmentStatus) {
        this.birthYear = birthYear;
        this.region = region;
        this.incomeBracket = incomeBracket;
        this.employmentStatus = employmentStatus;
    }

    public void addPoint(int amount) {
        this.point = (this.point == null ? 0 : this.point) + amount;
    }

    /** 포인트 직접 세팅 (음수 방지는 호출부에서 처리) */
    public void setPoint(int point) {
        this.point = point;
    }
}
