package com.youfare.external.publicdata;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ApiResponse;
import com.youfare.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSyncController {

    private final PublicDataSyncService syncService;

    // 관리자 키. 비어 있으면 엔드포인트는 항상 차단(외부 API 대량호출 abuse 방지).
    @Value("${admin.api-key:}")
    private String adminApiKey;

    @Operation(summary = "복지 데이터 수동 동기화",
            description = "공공데이터포털에서 복지 데이터를 즉시 가져옵니다. 헤더 X-ADMIN-KEY가 서버 설정과 일치해야 합니다.")
    @PostMapping("/welfare/sync")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sync(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String key) {
        // 키 미설정(빈 값)이거나 헤더가 일치하지 않으면 차단. 로그인 유저라도 관리자 키 없이는 호출 불가.
        if (!StringUtils.hasText(adminApiKey) || !adminApiKey.equals(key)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int count = syncService.syncAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("syncedCount", count)));
    }
}
