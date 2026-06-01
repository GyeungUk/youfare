package com.youfare.domain.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequest {

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 500, message = "메시지는 500자 이내로 입력해주세요.")
    private String message;
}
