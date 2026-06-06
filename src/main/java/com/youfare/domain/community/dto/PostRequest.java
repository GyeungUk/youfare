package com.youfare.domain.community.dto;

import com.youfare.domain.community.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 작성 요청.
 *
 * <p>이미지 첨부 때문에 작성 API는 multipart/form-data로 받는다.
 * 텍스트 필드는 {@code @ModelAttribute}로 바인딩되므로 {@code @Setter}가 필요하다.
 * (이미지 파일은 컨트롤러에서 별도 RequestParam으로 받는다.)
 */
@Getter
@Setter
@NoArgsConstructor
public class PostRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private PostCategory category;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    /**
     * 익명 작성 여부 (미지정 시 false).
     * 필드명을 'anonymous'로 둬야 Jackson 프로퍼티명도 'anonymous'가 되어
     * 응답(PostResponse.anonymous)과 요청 키가 일치한다.
     * (이전엔 'isAnonymous' 필드라 getter가 isAnonymous() → 프로퍼티 'anonymous'가 되면서
     *  프론트가 보내는 'isAnonymous' 키가 무시돼 익명 설정이 항상 false가 되는 버그가 있었다.)
     */
    private boolean anonymous;

    /** 연결할 복지 혜택 id (선택). 혜택 후기 글일 때만 사용. */
    private Long relatedWelfareId;
}
