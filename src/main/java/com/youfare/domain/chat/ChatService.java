package com.youfare.domain.chat;

import com.youfare.domain.scrap.PointPolicy;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.external.openai.OpenAiClient;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DISCLAIMER =
            "※ 본 답변은 참고용 정보이며, 정확한 내용과 신청 조건은 해당 기관 공식 채널에서 반드시 확인하시기 바랍니다.";

    private final OpenAiClient openAiClient;
    private final PersonalizedPromptBuilder promptBuilder;
    private final UserRepository userRepository;

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest request) {
        // 비로그인(게스트)은 userId == null — 프로필 없이 일반 상담 모드로 동작한다.
        User user = (userId != null)
                ? userRepository.findByIdWithLock(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                : null;

        // 개인화 시스템 프롬프트 생성 (로그인 → 프로필 기반 / 게스트 → 일반)
        String systemPrompt = (user != null) ? promptBuilder.build(user) : promptBuilder.buildAnonymous();

        // OpenAI API 호출
        String answer = openAiClient.chat(systemPrompt, request.getMessage());

        // 면책 고지는 AI가 생성하면 한자가 섞이거나 누락될 수 있어, 항상 고정 문구를 코드에서 덧붙인다.
        // (프론트는 answer만 렌더링하므로 본문에 합쳐 노출하고, disclaimer 필드는 API 호환용으로 함께 반환)
        String answerWithDisclaimer = answer + "\n\n" + DISCLAIMER;

        // 챗봇 사용 포인트 적립 (+1) — 로그인 유저만 (게스트는 적립 대상 없음)
        if (user != null) user.addPoint(PointPolicy.CHAT_USE);

        return ChatResponse.builder()
                .answer(answerWithDisclaimer)
                .disclaimer(DISCLAIMER)
                .build();
    }
}
