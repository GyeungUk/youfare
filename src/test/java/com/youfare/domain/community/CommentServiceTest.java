package com.youfare.domain.community;

import com.youfare.domain.community.dto.CommentRequest;
import com.youfare.domain.community.dto.CommentResponse;
import com.youfare.domain.user.Provider;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 댓글/대댓글 동작 검증 (STEP 9 완료 기준)
 * - 대댓글 2depth 고정, 초과 시 명확한 에러
 * - 댓글 + 대댓글 중첩 조회
 * - soft delete 시 내용만 "삭제된 댓글입니다"로 대체, 구조 유지
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceTest {

    @Autowired CommentService commentService;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        user = userRepository.save(User.builder()
                .socialId("c-test-" + ts).provider(Provider.KAKAO).nickname("댓글러").build());
        post = postRepository.save(Post.builder()
                .author(user).category(PostCategory.QUESTION)
                .title("질문 글").content("내용").build());
    }

    private CommentResponse comment(String content, Long parentId) {
        CommentRequest req = new CommentRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(req, "content", content);
        org.springframework.test.util.ReflectionTestUtils.setField(req, "parentId", parentId);
        return commentService.create(user.getId(), post.getId(), req);
    }

    @Test
    @DisplayName("댓글 → 대댓글까지는 허용, 대댓글에 답글 달면 2depth 초과 에러")
    void replyDepthFixedAtTwo() {
        CommentResponse parent = comment("부모 댓글", null);
        CommentResponse reply = comment("대댓글", parent.getId());   // 2depth OK

        assertThatThrownBy(() -> comment("대대댓글", reply.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.COMMENT_DEPTH_EXCEEDED));
    }

    @Test
    @DisplayName("댓글 목록은 대댓글이 children으로 중첩되어 조회된다")
    void nestedComments() {
        CommentResponse parent = comment("부모", null);
        comment("자식1", parent.getId());
        comment("자식2", parent.getId());
        comment("독립 댓글", null);

        List<CommentResponse> roots = commentService.getComments(post.getId());

        assertThat(roots).hasSize(2);                       // 최상위 댓글 2개
        assertThat(roots.get(0).getChildren()).hasSize(2);  // 첫 댓글에 대댓글 2개
        assertThat(roots.get(1).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("soft delete된 댓글은 내용만 '삭제된 댓글입니다'로 바뀌고 대댓글 구조는 유지된다")
    void softDeleteKeepsStructure() {
        CommentResponse parent = comment("곧 삭제될 댓글", null);
        comment("살아있는 대댓글", parent.getId());

        commentService.delete(user.getId(), post.getId(), parent.getId());

        List<CommentResponse> roots = commentService.getComments(post.getId());
        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).isDeleted()).isTrue();
        assertThat(roots.get(0).getContent()).isEqualTo("삭제된 댓글입니다");
        assertThat(roots.get(0).getChildren()).hasSize(1);  // 대댓글은 그대로 보존
        assertThat(roots.get(0).getChildren().get(0).getContent()).isEqualTo("살아있는 대댓글");
    }
}
