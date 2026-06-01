package com.youfare.external.publicdata;

import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSyncController {

    private final PublicDataSyncService syncService;

    @Operation(summary = "복지 데이터 수동 동기화", description = "공공데이터포털에서 복지 데이터를 즉시 가져옵니다.")
    @PostMapping("/welfare/sync")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sync() {
        int count = syncService.syncAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("syncedCount", count)));
    }
}
