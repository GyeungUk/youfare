package com.youfare.external.openai;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final String model;

    // baseUrl·인증 헤더가 설정된 싱글톤 WebClient 빈을 주입받아 재사용 (매 요청 재생성 방지)
    public OpenAiClient(WebClient openAiWebClient,
                        @Value("${openai.model}") String model) {
        this.openAiWebClient = openAiWebClient;
        this.model = model;
    }

    public String chat(String systemPrompt, String userMessage) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .maxTokens(1000)
                .messages(List.of(
                        ChatCompletionRequest.Message.builder().role("system").content(systemPrompt).build(),
                        ChatCompletionRequest.Message.builder().role("user").content(userMessage).build()
                ))
                .build();

        try {
            ChatCompletionResponse response = openAiWebClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            return response != null ? response.getContent() : "응답을 받지 못했습니다.";

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
