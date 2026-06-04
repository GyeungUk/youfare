package com.youfare.domain.welfare;

import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Welfare", description = "복지 혜택 조회 API")
@RestController
@RequestMapping("/welfare")
@RequiredArgsConstructor
public class WelfareController {

    private final WelfareService welfareService;

    @Operation(summary = "복지 목록 조회", description = "카테고리/지역/상태(진행중·마감·예정) 필터 + 페이징")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WelfareResponse>>> getList(
            @RequestParam(required = false) WelfareCategory category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) WelfareStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(welfareService.getList(category, region, status, pageable)));
    }

    @Operation(summary = "개인화 추천", description = "로그인 유저 프로필 기반 맞춤 혜택 (신청 가능 + 조건 부합)")
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<Page<WelfareResponse>>> getRecommended(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(welfareService.getRecommended(user, pageable)));
    }

    @Operation(summary = "복지 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WelfareResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(welfareService.getDetail(id)));
    }
}
