package com.youfare.domain.community;

import com.youfare.domain.community.dto.CommentResponse;
import com.youfare.domain.community.dto.PostListResponse;
import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 다른 도메인 경로(/welfare, /users/me) 아래에 노출되는 커뮤니티 연계 조회 API.
 * 게시글/댓글 도메인 로직이지만 URL이 /posts가 아니므로 별도 컨트롤러로 분리.
 */
@Tag(name = "Community Query", description = "혜택 연계 후기 / 마이페이지 게시글·댓글 조회 API")
@RestController
@RequiredArgsConstructor
public class CommunityQueryController {

    private final PostService postService;
    private final CommentService commentService;

    @Operation(summary = "혜택 후기 목록 조회", description = "특정 복지 혜택에 연결된 게시글 중 category=REVIEW만 조회. 혜택 상세 페이지의 '이 혜택 후기 N개' 섹션에 사용됩니다.")
    @GetMapping("/welfare/{welfareId}/posts")
    public ResponseEntity<ApiResponse<Page<PostListResponse>>> getWelfareReviews(
            @PathVariable Long welfareId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getWelfareReviews(welfareId, page)));
    }

    @Operation(summary = "내 게시글 목록", description = "마이페이지용. 로그인 유저가 작성한 게시글 목록(최신순).")
    @GetMapping("/users/me/posts")
    public ResponseEntity<ApiResponse<Page<PostListResponse>>> getMyPosts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getMyPosts(user.getId(), page)));
    }

    @Operation(summary = "내 댓글 목록", description = "마이페이지용. 로그인 유저가 작성한 댓글 목록(최신순, 글 제목 포함).")
    @GetMapping("/users/me/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getMyComments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getMyComments(user.getId(), page)));
    }
}
