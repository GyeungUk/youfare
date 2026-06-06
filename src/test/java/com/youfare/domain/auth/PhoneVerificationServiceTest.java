package com.youfare.domain.auth;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    @Mock JwtProvider jwtProvider;
    PhoneVerificationService service;

    private static final String PHONE = "01012345678";

    @BeforeEach
    void setUp() {
        service = new PhoneVerificationService(jwtProvider);
    }

    @Test
    @DisplayName("발급한 코드로 검증하면 phoneVerificationToken을 반환한다")
    void verify_success() {
        String code = service.issueCode(PHONE);
        given(jwtProvider.generatePhoneToken(PHONE)).willReturn("phone-token");

        String token = service.verifyCode(PHONE, code);

        assertThat(token).isEqualTo("phone-token");
    }

    @Test
    @DisplayName("발급 내역 없는 번호 검증 → PHONE_CODE_NOT_FOUND")
    void verify_noRequest() {
        assertThatThrownBy(() -> service.verifyCode(PHONE, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_CODE_NOT_FOUND);
    }

    @Test
    @DisplayName("코드 불일치 → PHONE_CODE_MISMATCH")
    void verify_wrongCode() {
        service.issueCode(PHONE);
        String wrong = "000000".equals(service.issueCode(PHONE)) ? "111111" : "000000";

        assertThatThrownBy(() -> service.verifyCode(PHONE, wrong))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_CODE_MISMATCH);
    }

    @Test
    @DisplayName("검증 성공 후 코드는 1회용 — 재사용 시 NOT_FOUND")
    void verify_codeIsSingleUse() {
        String code = service.issueCode(PHONE);
        given(jwtProvider.generatePhoneToken(PHONE)).willReturn("phone-token");
        service.verifyCode(PHONE, code);

        assertThatThrownBy(() -> service.verifyCode(PHONE, code))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_CODE_NOT_FOUND);
    }
}
