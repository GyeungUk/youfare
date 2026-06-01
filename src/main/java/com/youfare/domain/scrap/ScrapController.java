package com.youfare.domain.scrap;

import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Scrap", description = "복지 혜택 스크랩 API")
@RestController
@RequestMapping("/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @Operation(summary = "스크랩 추가", description = "혜택을 스크랩하고 포인트 +2를 획득합니다.")
    @PostMapping("/{welfareId}")
    public ResponseEntity<ApiResponse<ScrapResponse>> addScrap(
            @AuthenticationPrincipal User user,
            @PathVariable Long welfareId) {
        return ResponseEntity.ok(ApiResponse.ok(scrapService.addScrap(user.getId(), welfareId)));
    }

    @Operation(summary = "스크랩 해제")
    @DeleteMapping("/{welfareId}")
    public ResponseEntity<ApiResponse<Void>> removeScrap(
            @AuthenticationPrincipal User user,
            @PathVariable Long welfareId) {
        scrapService.removeScrap(user.getId(), welfareId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "내 스크랩 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ScrapResponse>>> getMyScrap(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(scrapService.getMyScrap(user.getId())));
    }
}
