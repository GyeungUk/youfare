package com.youfare.domain.community;

import com.youfare.domain.community.dto.CommentRequest;
import com.youfare.domain.community.dto.CommentResponse;
import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "커뮤니티 댓글 API")
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "parentId 없으면 댓글, 있으면 대댓글. 대댓글에 다시 답글을 달면(2depth 초과) 400 에러. 작성 성공 시 포인트 +4 적립.")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.create(user.getId(), postId, request)));
    }

    @Operation(summary = "댓글 목록 조회", description = "댓글 + 하위 대댓글을 중첩(children) 구조로 반환. 삭제된 댓글은 '삭제된 댓글입니다'로 표시되고 구조는 유지됩니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(postId)));
    }

    @Operation(summary = "댓글 삭제", description = "본인만 가능. Hard delete 대신 soft delete(isDeleted=true)로 처리해 대댓글 구조를 보존합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentService.delete(user.getId(), postId, commentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
