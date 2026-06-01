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

    // Post (Community)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "게시글을 찾을 수 없습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "P002", "본인이 작성한 게시글만 수정/삭제할 수 있습니다."),
    LIKE_CONFLICT(HttpStatus.CONFLICT, "P003", "좋아요 처리 중 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "CM002", "본인이 작성한 댓글만 삭제할 수 있습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CM003", "대댓글에는 다시 답글을 달 수 없습니다. (답글은 2단계까지만 가능합니다)"),
    COMMENT_POST_MISMATCH(HttpStatus.BAD_REQUEST, "CM004", "부모 댓글이 해당 게시글의 댓글이 아닙니다."),

    // External API
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E001", "외부 API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
