package com.youfare.external.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatCompletionRequest {

    private String model;
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    // 낮을수록 답변이 결정적·집중적이 된다. Llama 계열의 한자 섞임/산만함을 줄이는 핵심 파라미터.
    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @Getter
    @Builder
    public static class Message {
        private String role;    // "system" | "user" | "assistant"
        private String content;
    }
}
