package com.youfare.domain.chat;

import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "AI 개인화 챗봇 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final GuestChatRateLimiter guestRateLimiter;

    @Operation(
        summary = "AI 복지 상담 챗봇",
        description = "로그인 유저의 프로필(나이·지역·소득구간·취업상태)과, DB에서 실시간 조회한 맞춤 혜택 요약을 "
                + "시스템 프롬프트에 동적으로 주입해 개인화된 답변을 반환합니다. "
                + "PersonalizedPromptBuilder가 컨텍스트를 구성하므로 '같은 질문, 다른 유저 → 다른 답변'이 보장됩니다(RAG 패턴). "
                + "OpenAI Chat Completions API를 WebClient로 호출하며, 사용 시 포인트 +1이 적립됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        // 비로그인(게스트)은 IP 기준 하루 횟수 제한을 적용한 뒤 일반 상담 모드(userId=null)로 동작.
        // 로그인 유저는 제한 없이 개인화 상담.
        if (user == null) {
            guestRateLimiter.checkAndConsume(GuestChatRateLimiter.clientIp(httpRequest));
            return ResponseEntity.ok(ApiResponse.ok(chatService.chat(null, request)));
        }
        return ResponseEntity.ok(ApiResponse.ok(chatService.chat(user.getId(), request)));
    }
}
