package com.youfare.domain.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PhoneSendRequest {

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[016789][0-9]{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phoneNumber;
}
