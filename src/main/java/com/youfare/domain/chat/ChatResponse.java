package com.youfare.domain.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {
    private String answer;
    private String disclaimer;
}
