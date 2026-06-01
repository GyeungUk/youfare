package com.youfare.domain.chat;

import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(
        summary = "AI 복지 상담 챗봇",
        description = "로그인 유저의 프로필과 맞춤 혜택을 AI 컨텍스트에 주입해 개인화된 답변을 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.chat(user.getId(), request)));
    }
}
