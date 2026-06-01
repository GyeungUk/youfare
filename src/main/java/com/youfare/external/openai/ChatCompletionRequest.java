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

    @Getter
    @Builder
    public static class Message {
        private String role;    // "system" | "user" | "assistant"
        private String content;
    }
}
