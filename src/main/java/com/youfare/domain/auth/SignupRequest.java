package com.youfare.domain.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[016789][0-9]{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phoneNumber;

    /** 전화 인증 단계에서 받은 단기 토큰. 서버가 검증해 인증된 번호임을 확인한다. */
    @NotBlank(message = "전화번호 인증이 필요합니다.")
    private String phoneVerificationToken;

    /** 필수 약관 동의 (이용약관) */
    private boolean agreeTerms;

    /** 필수 약관 동의 (개인정보 처리방침) */
    private boolean agreePrivacy;

    /** 선택 동의 (마케팅 수신) */
    private boolean agreeMarketing;

    @AssertTrue(message = "이용약관에 동의해야 합니다.")
    public boolean isAgreeTerms() {
        return agreeTerms;
    }

    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
    public boolean isAgreePrivacy() {
        return agreePrivacy;
    }
}
