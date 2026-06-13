package com.youfare.domain.auth;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock JwtProvider jwtProvider;
    @Mock JavaMailSender mailSender;
    EmailVerificationService service;

    private static final String EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(jwtProvider, mailSender);
        ReflectionTestUtils.setField(service, "codeTtlSeconds", 180L);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@test.com");
    }

    /** 발송된 메일 본문에서 6자리 코드를 추출한다. */
    private String sendAndCaptureCode(String email) {
        service.issueCode(email);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{6})").matcher(body);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    @DisplayName("발급한 코드로 검증하면 emailVerificationToken을 반환한다")
    void verify_success() {
        String code = sendAndCaptureCode(EMAIL);
        given(jwtProvider.generateEmailToken(EMAIL)).willReturn("email-token");

        String token = service.verifyCode(EMAIL, code);

        assertThat(token).isEqualTo("email-token");
    }

    @Test
    @DisplayName("발급 내역 없는 이메일 검증 → EMAIL_CODE_NOT_FOUND")
    void verify_noRequest() {
        assertThatThrownBy(() -> service.verifyCode(EMAIL, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_NOT_FOUND);
    }

    @Test
    @DisplayName("코드 불일치 → EMAIL_CODE_MISMATCH")
    void verify_wrongCode() {
        String code = sendAndCaptureCode(EMAIL);
        String wrong = code.equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> service.verifyCode(EMAIL, wrong))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_MISMATCH);
    }

    @Test
    @DisplayName("검증 성공 후 코드는 1회용 — 재사용 시 NOT_FOUND")
    void verify_codeIsSingleUse() {
        String code = sendAndCaptureCode(EMAIL);
        given(jwtProvider.generateEmailToken(EMAIL)).willReturn("email-token");
        service.verifyCode(EMAIL, code);

        assertThatThrownBy(() -> service.verifyCode(EMAIL, code))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_NOT_FOUND);
    }

    @Test
    @DisplayName("메일 발송 실패 → MAIL_SEND_FAILED, 코드 저장도 롤백되어 이후 검증은 NOT_FOUND")
    void issue_mailSendFails() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.issueCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MAIL_SEND_FAILED);
    }
}
