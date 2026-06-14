package com.youfare.domain.auth;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock JwtProvider jwtProvider;
    // 발송은 Brevo HTTP API(WebClient)로 한다. 플루언트 체인을 깊은 스텁으로 모킹한다.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) WebClient brevoWebClient;
    EmailVerificationService service;

    private static final String EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                jwtProvider, brevoWebClient, "xkeysib-test", "noreply@test.com", "YouFare", 180L, 30L);
    }

    /** Brevo 호출이 성공하도록 스텁하고, 전송 본문(textContent)에서 6자리 코드를 추출한다. */
    private String sendAndCaptureCode(String email) {
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        given(brevoWebClient.post().uri(anyString()).bodyValue(bodyCaptor.capture())
                .retrieve().bodyToMono(Void.class)).willReturn(Mono.empty());

        service.issueCode(email);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) bodyCaptor.getValue();
        Matcher m = Pattern.compile("(\\d{6})").matcher((String) body.get("textContent"));
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
        given(brevoWebClient.post().uri(anyString()).bodyValue(any())
                .retrieve().bodyToMono(Void.class))
                .willReturn(Mono.error(new RuntimeException("brevo down")));

        assertThatThrownBy(() -> service.issueCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MAIL_SEND_FAILED);

        // 발송 실패 시 코드가 저장돼 있으면 안 된다.
        assertThatThrownBy(() -> service.verifyCode(EMAIL, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_NOT_FOUND);
    }
}
