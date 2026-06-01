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
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 개인화 시스템 프롬프트 생성
        String systemPrompt = promptBuilder.build(user);

        // OpenAI API 호출
        String answer = openAiClient.chat(systemPrompt, request.getMessage());

        // 챗봇 사용 포인트 적립 (+1)
        user.addPoint(PointPolicy.CHAT_USE);

        return ChatResponse.builder()
                .answer(answer)
                .disclaimer(DISCLAIMER)
                .build();
    }
}
