package com.youfare.domain.community;

import com.youfare.domain.community.dto.*;
import com.youfare.domain.user.User;
import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Post", description = "커뮤니티 게시글 API")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;

    @Operation(summary = "게시글 작성",
            description = "로그인 필요. multipart/form-data로 전송합니다. "
                    + "텍스트 필드(category·title·content·anonymous·relatedWelfareId)와 함께 "
                    + "attachments로 첨부물을 최대 10개(각 20MB·총 50MB) 올릴 수 있습니다. "
                    + "이미지(jpg·png·gif·webp)·동영상(mp4·webm)·문서(pdf·doc·docx·xls·xlsx·ppt·pptx·hwp·hwpx·txt·csv·zip) 지원. "
                    + "첨부는 선택. 작성 성공 시 포인트 +5 적립. relatedWelfareId로 혜택 후기를 연결할 수 있습니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute PostRequest request,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {
        return ResponseEntity.ok(ApiResponse.ok(postService.create(user.getId(), request, attachments)));
    }

    @Operation(summary = "게시글 목록 조회",
            description = "카테고리 필터 + 페이징(size=10). sort=likes 면 좋아요 내림차순, 기본은 최신순. 익명 글은 작성자가 '익명 청년'으로 마스킹됩니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostListResponse>>> getList(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getList(category, sort, page)));
    }

    @Operation(summary = "인기글 조회", description = "최근 7일 내 작성된 글 중 (likeCount * 2 + viewCount) 기준 상위 10개.")
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<PostListResponse>>> getHotPosts() {
        return ResponseEntity.ok(ApiResponse.ok(postService.getHotPosts()));
    }

    @Operation(summary = "게시글 상세 조회", description = "조회 시 viewCount +1. 연결된 복지 혜택이 있으면 혜택 요약을 함께 반환합니다. 게스트(비로그인)도 열람 가능하며, 이 경우 liked/mine은 false로 내려갑니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        // 게스트는 user가 null — liked/mine 계산이 null-safe하므로 userId만 null로 넘긴다.
        Long userId = (user != null) ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(postService.getDetail(id, userId)));
    }

    @Operation(summary = "게시글 수정", description = "본인만 가능. 제목/내용만 수정됩니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.update(user.getId(), id, request)));
    }

    @Operation(summary = "게시글 삭제", description = "본인만 가능. Hard delete (연관 댓글·좋아요도 함께 삭제).")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        postService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "게시글 좋아요 토글",
            description = "좋아요가 없으면 추가, 있으면 취소합니다. "
                    + "Post.likeCount는 동시 좋아요 시 race condition이 생기므로 낙관적 락(@Version)으로 보호하고, "
                    + "ObjectOptimisticLockingFailureException 발생 시 최대 3회 재시도합니다. "
                    + "비관적 락 대신 낙관적 락을 쓴 이유: 좋아요는 충돌 빈도가 낮고 재시도 비용이 적은 반면, "
                    + "비관적 락은 DB 락 점유 시간이 길어 처리량(throughput)이 떨어질 수 있기 때문입니다. "
                    + "(post_id, user_id) unique 제약이 중복 좋아요의 최종 방어선입니다."
    )
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(postLikeService.toggleLike(user.getId(), id)));
    }
}
