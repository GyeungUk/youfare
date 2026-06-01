package com.youfare.domain.user;

import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 온보딩 및 마이페이지 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(user.getId())));
    }

    @Operation(summary = "온보딩 정보 저장/수정", description = "생년, 지역, 소득구간, 취업상태를 입력합니다.")
    @PutMapping("/me/onboarding")
    public ResponseEntity<ApiResponse<UserResponse>> updateOnboarding(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateOnboarding(user.getId(), request)));
    }
}
