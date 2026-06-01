package com.youfare.external.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {

    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }

    public String getContent() {
        if (choices == null || choices.isEmpty()) return "";
        Message message = choices.get(0).getMessage();
        return (message != null && message.getContent() != null) ? message.getContent() : "";
    }
}
