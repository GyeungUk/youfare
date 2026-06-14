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

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 가입된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "A004", "필수 약관에 동의해야 가입할 수 있습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "해당 정보로 가입된 계정을 찾을 수 없습니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A006", "이미 사용 중인 아이디입니다."),
    // 같은 이메일이 소셜(네이버/카카오)로 이미 가입된 경우. 실제 메시지는 provider 이름을 넣어 동적으로 던진다.
    SOCIAL_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "A007", "소셜 로그인으로 가입된 이메일입니다."),

    // Email verification
    EMAIL_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "EV001", "인증 요청 내역이 없습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "EV002", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "EV003", "인증번호가 올바르지 않습니다."),
    EMAIL_CODE_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "EV004", "인증 시도 횟수를 초과했습니다. 다시 요청해주세요."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EV005", "이메일 인증이 필요합니다."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EV006", "인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // Welfare
    WELFARE_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "복지 혜택을 찾을 수 없습니다."),

    // Chat
    GUEST_CHAT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CH001",
            "비로그인 상태에서는 AI 상담 횟수가 제한돼요. 로그인하면 계속 이용할 수 있어요."),

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

    // File / Media upload
    INVALID_MEDIA_UPLOAD(HttpStatus.BAD_REQUEST, "F001", "첨부 파일은 최대 10개, 각 20MB·총 50MB 이하로 업로드해 주세요."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "F002", "지원하지 않는 형식입니다. 이미지(jpg·png·gif·webp)·동영상(mp4·webm)·문서(pdf·doc·docx·xls·xlsx·ppt·pptx·hwp·hwpx·txt·csv·zip)만 올릴 수 있어요."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 업로드에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // External API
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E001", "외부 API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
