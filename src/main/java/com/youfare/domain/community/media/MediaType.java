package com.youfare.domain.community.media;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * 인라인 표시되는 미디어(이미지·동영상)의 매직넘버(파일 시그니처) 판별기.
 *
 * <p>확장자/Content-Type 헤더는 클라이언트가 위조할 수 있으므로 믿지 않고,
 * 파일의 실제 첫 바이트(매직넘버)로 타입을 판별한다. {@code <img>}·{@code <video>}로
 * 브라우저가 직접 렌더링하는 형식이라 위조 시 위험이 커서 실검증한다.
 *
 * <p>다운로드로만 제공되는 일반 문서(pdf·docx 등)는 여기서 다루지 않고
 * {@link MediaUploadValidator}가 확장자 화이트리스트로 처리한다.
 */
@Getter
@RequiredArgsConstructor
public enum MediaType {

    JPEG(MediaKind.IMAGE, "image/jpeg", "jpg"),
    PNG(MediaKind.IMAGE, "image/png", "png"),
    GIF(MediaKind.IMAGE, "image/gif", "gif"),
    WEBP(MediaKind.IMAGE, "image/webp", "webp"),
    MP4(MediaKind.VIDEO, "video/mp4", "mp4"),
    WEBM(MediaKind.VIDEO, "video/webm", "webm");

    private final MediaKind kind;
    private final String mimeType;
    private final String extension;

    /**
     * 매직넘버로 이미지·동영상 타입을 판별한다. 해당 없으면 {@link Optional#empty()}.
     *
     * <ul>
     *   <li>JPEG: FF D8 FF</li>
     *   <li>PNG : 89 50 4E 47</li>
     *   <li>GIF : 47 49 46 38 ("GIF8")</li>
     *   <li>WEBP: "RIFF"....​"WEBP" (0..3 = RIFF, 8..11 = WEBP)</li>
     *   <li>MP4 : 4..7 = 66 74 79 70 ("ftyp")</li>
     *   <li>WEBM: 1A 45 DF A3 (EBML)</li>
     * </ul>
     */
    public static Optional<MediaType> detect(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return Optional.empty();
        }
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;

        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return Optional.of(JPEG);
        }
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
            return Optional.of(PNG);
        }
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) {
            return Optional.of(GIF);
        }
        // RIFF....WEBP
        if (b0 == 'R' && b1 == 'I' && b2 == 'F' && b3 == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return Optional.of(WEBP);
        }
        // mp4: ftyp box at offset 4
        if (bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
            return Optional.of(MP4);
        }
        // webm/matroska EBML header
        if (b0 == 0x1A && b1 == 0x45 && b2 == 0xDF && b3 == 0xA3) {
            return Optional.of(WEBM);
        }
        return Optional.empty();
    }
}
