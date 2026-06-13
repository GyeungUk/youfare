package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/** 인증번호 발급 응답. 실제 메일로 코드가 발송되므로 코드 자체는 응답에 담지 않는다. */
@Getter
@Builder
public class EmailSendResponse {

    private boolean sent;
    private int ttlSeconds;
}
