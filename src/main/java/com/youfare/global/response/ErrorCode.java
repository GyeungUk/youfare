package com.youfare.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),

    // Welfare
    WELFARE_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "복지 혜택을 찾을 수 없습니다."),

    // Scrap
    ALREADY_SCRAPPED(HttpStatus.CONFLICT, "S001", "이미 스크랩한 혜택입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "스크랩을 찾을 수 없습니다."),

    // External API
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E001", "외부 API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
