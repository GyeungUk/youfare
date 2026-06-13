package com.youfare.domain.user;

import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                // 같은 provider 안에서 socialId / email 중복 가입을 DB 차원에서 차단.
                // (소셜 유저의 email은 null일 수 있으나 Postgres는 null을 서로 다른 값으로 취급해 무방)
                @UniqueConstraint(name = "uk_users_social", columnNames = {"socialId", "provider"}),
                @UniqueConstraint(name = "uk_users_email_provider", columnNames = {"email", "provider"}),
                // 폼 로그인 아이디 중복 차단. 소셜 유저는 username이 null이며 Postgres가 null을 서로 다른 값으로 취급해 무방.
                @UniqueConstraint(name = "uk_users_username_provider", columnNames = {"username", "provider"})
        }
)
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

    /** 폼(LOCAL) 로그인 아이디. 소셜 로그인 유저는 null. 가입 시 소문자로 정규화해 저장한다. */
    private String username;

    /** 폼(LOCAL) 로그인용 BCrypt 해시. 소셜 로그인 유저는 null. */
    private String password;

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

    /**
     * 폼 회원가입용 LOCAL 유저 생성.
     * socialId 컬럼은 NOT NULL이라 email을 식별자로 채워 제약을 만족시킨다.
     */
    public static User ofLocal(String email, String username, String encodedPassword, String nickname) {
        return User.builder()
                .socialId(email)
                .provider(Provider.LOCAL)
                .email(email)
                .username(username)
                .password(encodedPassword)
                .nickname(nickname)
                .build();
    }

    /** 비밀번호 재설정 — 이미 BCrypt로 해시된 값을 받는다. */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

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
