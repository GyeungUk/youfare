package com.youfare.domain.community.media;

import com.youfare.domain.community.media.MediaUploadValidator.ValidatedMedia;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 첨부물 업로드 정책 검증기 단위 테스트.
 * 이미지·동영상 매직넘버 실검증 + 문서 확장자 화이트리스트 + 개수/용량 정책을 확인한다.
 */
class MediaUploadValidatorTest {

    private final MediaUploadValidator validator = new MediaUploadValidator();

    /** 지정 크기의 바이트 배열 앞에 PNG 시그니처를 박아 "유효한 PNG"를 만든다. */
    private static byte[] png(int size) {
        byte[] b = new byte[Math.max(size, 16)];
        byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(sig, 0, b, 0, sig.length);
        return b;
    }

    private static MultipartFile pngFile(byte[] bytes) {
        return new MockMultipartFile("attachments", "img.png", "image/png", bytes);
    }

    @Test
    @DisplayName("첨부가 없으면 빈 리스트를 반환한다 (첨부는 선택)")
    void emptyWhenNoFiles() {
        assertThat(validator.validate(null)).isEmpty();
        assertThat(validator.validate(List.of())).isEmpty();
    }

    @Test
    @DisplayName("유효한 PNG는 통과하고 IMAGE 종류·판별 타입을 돌려준다")
    void validPngPasses() {
        List<ValidatedMedia> result = validator.validate(List.of(pngFile(png(1024))));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo(MediaKind.IMAGE);
        assertThat(result.get(0).contentType()).isEqualTo("image/png");
        assertThat(result.get(0).extension()).isEqualTo("png");
    }

    @Test
    @DisplayName("mp4(ftyp 시그니처)는 VIDEO로 통과한다")
    void validMp4Passes() {
        byte[] mp4 = new byte[16];
        byte[] sig = {0, 0, 0, 0x18, 'f', 't', 'y', 'p'};
        System.arraycopy(sig, 0, mp4, 0, sig.length);
        MultipartFile file = new MockMultipartFile("attachments", "clip.mp4", "video/mp4", mp4);

        List<ValidatedMedia> result = validator.validate(List.of(file));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo(MediaKind.VIDEO);
        assertThat(result.get(0).extension()).isEqualTo("mp4");
    }

    @Test
    @DisplayName("pdf는 확장자 화이트리스트로 FILE 종류로 통과한다 (원본 파일명 보존)")
    void pdfPassesAsFile() {
        // 매직넘버 미일치(텍스트)지만 확장자가 화이트리스트에 있어 FILE로 허용된다.
        MultipartFile file = new MockMultipartFile(
                "attachments", "이력서.pdf", "application/pdf", "%PDF dummy".getBytes());

        List<ValidatedMedia> result = validator.validate(List.of(file));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo(MediaKind.FILE);
        assertThat(result.get(0).extension()).isEqualTo("pdf");
        assertThat(result.get(0).originalName()).isEqualTo("이력서.pdf");
    }

    @Test
    @DisplayName("11개 이상이면 INVALID_MEDIA_UPLOAD")
    void rejectsTooMany() {
        List<MultipartFile> eleven = new ArrayList<>();
        for (int i = 0; i < 11; i++) eleven.add(pngFile(png(16)));

        assertThatThrownBy(() -> validator.validate(eleven))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MEDIA_UPLOAD);
    }

    @Test
    @DisplayName("1개가 20MB를 초과하면 INVALID_MEDIA_UPLOAD")
    void rejectsOversizedFile() {
        MultipartFile big = pngFile(png(20 * 1024 * 1024 + 1));

        assertThatThrownBy(() -> validator.validate(List.of(big)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MEDIA_UPLOAD);
    }

    @Test
    @DisplayName("총합이 50MB를 초과하면 INVALID_MEDIA_UPLOAD (각 파일은 20MB 이하라도)")
    void rejectsOverTotalSize() {
        // 18MB × 3 = 54MB. 각 파일은 20MB 이하지만 총합이 50MB를 넘는다.
        int each = 18 * 1024 * 1024;
        List<MultipartFile> files = List.of(pngFile(png(each)), pngFile(png(each)), pngFile(png(each)));

        assertThatThrownBy(() -> validator.validate(files))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MEDIA_UPLOAD);
    }

    @Test
    @DisplayName("확장자가 png여도 내용이 이미지가 아니고 문서 화이트리스트에도 없으면 UNSUPPORTED_MEDIA_TYPE")
    void rejectsForgedImage() {
        // image/png로 위장했지만 매직넘버 불일치 + png는 문서 화이트리스트가 아님 → 차단.
        MultipartFile forged = new MockMultipartFile(
                "attachments", "evil.png", "image/png", "<svg>not an image</svg>".getBytes());

        assertThatThrownBy(() -> validator.validate(List.of(forged)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("화이트리스트 밖 확장자(실행 파일 등)는 UNSUPPORTED_MEDIA_TYPE")
    void rejectsExecutable() {
        MultipartFile exe = new MockMultipartFile(
                "attachments", "malware.exe", "application/octet-stream", "MZ binary".getBytes());

        assertThatThrownBy(() -> validator.validate(List.of(exe)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }
}
