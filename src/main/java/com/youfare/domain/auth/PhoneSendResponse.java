package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증번호 발급 응답.
 * devCode는 실제 SMS 대신 데모에서 코드를 화면에 보여주기 위한 필드(운영 전환 시 제거).
 */
@Getter
@Builder
public class PhoneSendResponse {

    private boolean sent;
    private int ttlSeconds;
    private String devCode;
}
